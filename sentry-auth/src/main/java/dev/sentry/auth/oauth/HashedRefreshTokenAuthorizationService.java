package dev.sentry.auth.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Autorizações em {@code oauth2_authorization} com o refresh token gravado só como hash SHA-256 (RN-F1-06.2) e
 * detecção de reuso: um refresh token já rotacionado revoga a autorização inteira (RN-F1-06.4).
 */
public class HashedRefreshTokenAuthorizationService extends JdbcOAuth2AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(HashedRefreshTokenAuthorizationService.class);
    private static final String HASH_PREFIX = "sha256:";

    private final JdbcTemplate jdbcTemplate;

    public HashedRefreshTokenAuthorizationService(JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        super(jdbcTemplate, registeredClientRepository);
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Grava a autorização com o refresh token em hash. Se o refresh token mudou (rotação), guarda o hash do anterior
     * para reconhecer um reuso.
     *
     * @param authorization autorização com o refresh token em claro ou já em hash
     */
    @Override
    public void save(OAuth2Authorization authorization) {
        OAuth2Authorization hashed = withRefreshTokenValue(authorization, hashOf(authorization));
        List<String> stored = jdbcTemplate.queryForList(
                "SELECT refresh_token_value FROM oauth2_authorization WHERE id = ?", String.class, authorization.getId());
        super.save(hashed);
        String newHash = hashOf(authorization);
        if (!stored.isEmpty() && stored.getFirst() != null && !stored.getFirst().equals(newHash)) {
            jdbcTemplate.update("INSERT INTO oauth2_rotated_refresh_tokens (token_hash, authorization_id) VALUES (?, ?)",
                    stored.getFirst(), authorization.getId());
        }
    }

    /**
     * Busca pelo token. Para refresh token, procura pelo hash e devolve a autorização com o valor em claro, que é o que
     * o Authorization Server compara. Um refresh token já rotacionado revoga a autorização e não é encontrado.
     *
     * @param token valor apresentado pelo cliente
     * @param tokenType tipo do token, ou {@code null} para procurar em todos
     * @return a autorização, ou {@code null}
     */
    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        if (tokenType != null && !OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            return super.findByToken(token, tokenType);
        }
        if (tokenType == null) {
            OAuth2Authorization other = super.findByToken(token, null);
            if (other != null) {
                return other;
            }
        }
        String hash = hash(token);
        OAuth2Authorization authorization = super.findByToken(hash, OAuth2TokenType.REFRESH_TOKEN);
        if (authorization != null) {
            return withRefreshTokenValue(authorization, token);
        }
        revokeIfRotated(hash);
        return null;
    }

    private void revokeIfRotated(String hash) {
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT authorization_id FROM oauth2_rotated_refresh_tokens WHERE token_hash = ?", String.class, hash);
        if (ids.isEmpty()) {
            return;
        }
        OAuth2Authorization authorization = findById(ids.getFirst());
        if (authorization != null) {
            remove(authorization);
        }
        log.atWarn().addKeyValue("authorization_id", ids.getFirst())
                .addKeyValue("client_id", authorization == null ? null : authorization.getRegisteredClientId())
                .log("Refresh token já rotacionado foi reapresentado; a autorização foi revogada");
    }

    private static String hashOf(OAuth2Authorization authorization) {
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        return refreshToken == null ? null : hash(refreshToken.getToken().getTokenValue());
    }

    private static OAuth2Authorization withRefreshTokenValue(OAuth2Authorization authorization, String value) {
        OAuth2Authorization.Token<OAuth2RefreshToken> current = authorization.getRefreshToken();
        if (current == null || current.getToken().getTokenValue().equals(value)) {
            return authorization;
        }
        OAuth2RefreshToken token = new OAuth2RefreshToken(value, current.getToken().getIssuedAt(),
                current.getToken().getExpiresAt());
        return OAuth2Authorization.from(authorization)
                .token(token, metadata -> metadata.putAll(current.getMetadata()))
                .build();
    }

    // valor já em hash fica como está, para findById seguido de save não aplicar o hash duas vezes
    static String hash(String value) {
        if (value.startsWith(HASH_PREFIX)) {
            return value;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
            return HASH_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
