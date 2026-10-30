package dev.sentry.auth;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** Dados de teste dos fluxos OAuth: organização, sistema cliente e usuário com papel nele, criados por SQL. */
public final class OAuthFixtures {

    public static final String PASSWORD = "S3nha!forte";
    public static final String REDIRECT_URI = "http://127.0.0.1:3000/callback";
    public static final String LOGOUT_URI = "http://127.0.0.1:3000/";
    /** Tela de login do front-end ({@code sentry.login-url}), para onde vai quem não tem sessão. */
    public static final String LOGIN_URL = "http://localhost:5173/login";
    /** PKCE do exemplo da RFC 7636: o desafio S256 de {@link #CODE_VERIFIER}. */
    public static final String CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    public static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    private OAuthFixtures() {
    }

    /**
     * URL de autorização com os parâmetros na query string, como o navegador envia (o Authorization Server os lê de lá).
     * Sai codificada; use com {@code get(URI.create(url))}, que não codifica de novo.
     *
     * @param extra pares nome/valor adicionais, como {@code "nonce", "abc"}
     */
    public static String authorizeUrl(String clientId, String scope, String... extra) {
        var builder = UriComponentsBuilder.fromPath("/oauth2/authorize")
                .queryParam("response_type", "code").queryParam("client_id", clientId)
                .queryParam("redirect_uri", REDIRECT_URI).queryParam("scope", scope).queryParam("state", "xyz")
                .queryParam("code_challenge", CODE_CHALLENGE).queryParam("code_challenge_method", "S256");
        for (int i = 0; i < extra.length; i += 2) {
            builder.queryParam(extra[i], extra[i + 1]);
        }
        return builder.encode().build().toUriString();
    }

    /**
     * Cria a organização {@code ORG_<code>} e o sistema {@code code} como cliente, com URIs de login e de logout.
     *
     * @return o id do sistema
     */
    public static long createClient(JdbcTemplate jdbc, String code, String clientType, String secretHash,
            boolean trusted, String scopes) {
        jdbc.update("""
                INSERT INTO organizations (name, external_code, uuid, is_active, is_deleted)
                VALUES (?, ?, gen_random_uuid(), TRUE, FALSE)""", "Org " + code, "ORG_" + code);
        jdbc.update("""
                INSERT INTO systems (id_organization, external_code, description, scopes, client_type, client_secret_hash,
                                     is_trusted, uuid, is_active, is_deleted)
                SELECT id_organization, ?, ?, ?, ?, ?, ?, gen_random_uuid(), TRUE, FALSE
                FROM organizations WHERE external_code = ?""",
                code, "Portal " + code, scopes, clientType, secretHash, trusted, "ORG_" + code);
        long idSystem = jdbc.queryForObject("SELECT id_system FROM systems WHERE external_code = ?", Long.class, code);
        jdbc.update("INSERT INTO system_redirect_uris (id_system, uri, uri_type) VALUES (?, ?, 'LOGIN'), (?, ?, 'LOGOUT')",
                idSystem, REDIRECT_URI, idSystem, LOGOUT_URI);
        return idSystem;
    }

    /** Dá ao usuário um papel ativo em mais um sistema. */
    public static void grantRole(JdbcTemplate jdbc, String username, String code) {
        jdbc.update("""
                INSERT INTO roles (id_system, id_organization, system_role, uuid, is_active, is_deleted)
                SELECT id_system, id_organization, ?, gen_random_uuid(), TRUE, FALSE FROM systems WHERE external_code = ?""",
                code + ".USER." + username, code);
        jdbc.update("""
                INSERT INTO role_users (id_role, id_user, is_active, is_deleted)
                SELECT r.id_role, u.id_user, TRUE, FALSE FROM roles r, users u
                WHERE r.system_role = ? AND u.username = ?""", code + ".USER." + username, username);
    }

    /** Remove, para testes sem transação, tudo o que os fixtures criaram para estes sistemas e usuários. */
    public static void cleanUp(JdbcTemplate jdbc, List<String> codes, List<String> usernames) {
        for (String code : codes) {
            jdbc.update("DELETE FROM oauth2_authorization WHERE registered_client_id = ?", code);
            jdbc.update("DELETE FROM access_logs WHERE id_system IN (SELECT id_system FROM systems WHERE external_code = ?)", code);
            jdbc.update("DELETE FROM role_users WHERE id_role IN (SELECT r.id_role FROM roles r JOIN systems s ON s.id_system = r.id_system WHERE s.external_code = ?)", code);
            jdbc.update("DELETE FROM roles WHERE id_system IN (SELECT id_system FROM systems WHERE external_code = ?)", code);
            jdbc.update("DELETE FROM system_redirect_uris WHERE id_system IN (SELECT id_system FROM systems WHERE external_code = ?)", code);
            jdbc.update("DELETE FROM systems WHERE external_code = ?", code);
            jdbc.update("DELETE FROM organizations WHERE external_code = ?", "ORG_" + code);
        }
        for (String username : usernames) {
            jdbc.update("DELETE FROM access_logs WHERE id_user IN (SELECT id_user FROM users WHERE username = ?)", username);
            jdbc.update("DELETE FROM spring_session WHERE principal_name IN (SELECT uuid::text FROM users WHERE username = ?)", username);
            jdbc.update("DELETE FROM users WHERE username = ?", username);
        }
    }

    /**
     * Cria um usuário {@code DATABASE} ativo, com um papel ativo no sistema {@code code}.
     *
     * @return o uuid do usuário
     */
    public static String createUserWithRole(JdbcTemplate jdbc, String username, String passwordHash, String code) {
        jdbc.update("""
                INSERT INTO users (uuid, name, authentication_type, username, password, email, is_active, is_deleted)
                VALUES (gen_random_uuid(), ?, 'DATABASE', ?, ?, ?, TRUE, FALSE)""",
                "Nome " + username, username, passwordHash, username + "@sentry.dev");
        jdbc.update("""
                INSERT INTO roles (id_system, id_organization, system_role, uuid, is_active, is_deleted)
                SELECT id_system, id_organization, ?, gen_random_uuid(), TRUE, FALSE FROM systems WHERE external_code = ?""",
                code + ".USER." + username, code);
        jdbc.update("""
                INSERT INTO role_users (id_role, id_user, is_active, is_deleted)
                SELECT r.id_role, u.id_user, TRUE, FALSE FROM roles r, users u
                WHERE r.system_role = ? AND u.username = ?""", code + ".USER." + username, username);
        return jdbc.queryForObject("SELECT uuid::text FROM users WHERE username = ?", String.class, username);
    }
}
