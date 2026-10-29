package dev.sentry.auth.oauth;

import static dev.sentry.auth.OAuthFixtures.CODE_VERIFIER;
import static dev.sentry.auth.OAuthFixtures.PASSWORD;
import static dev.sentry.auth.OAuthFixtures.authorizeUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dev.sentry.api.security.password.PasswordService;
import dev.sentry.auth.OAuthFixtures;
import dev.sentry.auth.PkceFlow;
import dev.sentry.auth.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class TokenClaimsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private EntityManager entityManager;

    private String userUuid;

    @BeforeEach
    void createClientAndUser() {
        OAuthFixtures.createClient(jdbcTemplate, "CLAIMS", "PUBLIC", null, false, "openid profile email");
        userUuid = OAuthFixtures.createUserWithRole(jdbcTemplate, "claims.user", passwordService.hash(PASSWORD), "CLAIMS");
    }

    private JWTClaimsSet[] login(String scope, String... extra) throws Exception {
        String code = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("CLAIMS", scope, extra), "claims.user", PASSWORD)
                .code();
        var tokens = PkceFlow.exchange(mockMvc, "CLAIMS", null, code, CODE_VERIFIER).andExpect(status().isOk());
        SignedJWT accessToken = verified(PkceFlow.json(tokens, "$.access_token"));
        SignedJWT idToken = verified(PkceFlow.json(tokens, "$.id_token"));
        return new JWTClaimsSet[] {accessToken.getJWTClaimsSet(), idToken.getJWTClaimsSet()};
    }

    // CA-F1-07.2: a assinatura confere com a chave do JWKS indicada pelo kid, com uma biblioteca JWT comum
    private SignedJWT verified(String token) throws Exception {
        SignedJWT jwt = SignedJWT.parse(token);
        JWKSet jwks = JWKSet.parse(mockMvc.perform(get("/oauth2/jwks")).andReturn().getResponse().getContentAsString());
        RSAKey key = (RSAKey) jwks.getKeyByKeyId(jwt.getHeader().getKeyID());
        assertThat(key).as("kid do token no JWKS").isNotNull();
        assertThat(jwt.verify(new RSASSAVerifier(key))).isTrue();
        assertEquals("RS256", jwt.getHeader().getAlgorithm().getName());
        return jwt;
    }

    // CA-F1-05.1, CA-F1-05.5, RN-F1-05.6, RN-F1-05.8
    @Test
    void accessTokenHasRequiredClaims() throws Exception {
        String accessToken = PkceFlow.json(PkceFlow.exchange(mockMvc, "CLAIMS", null,
                PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("CLAIMS", "openid profile email"), "claims.user",
                        PASSWORD).code(), CODE_VERIFIER), "$.access_token");
        SignedJWT jwt = verified(accessToken);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        String orgUuid = jdbcTemplate.queryForObject(
                "SELECT uuid::text FROM organizations WHERE external_code = 'ORG_CLAIMS'", String.class);

        assertEquals("at+jwt", jwt.getHeader().getType().toString());
        assertEquals("http://localhost:8081", claims.getIssuer());
        assertEquals(userUuid, claims.getSubject());
        assertThat(claims.getAudience()).containsExactly("CLAIMS");
        assertEquals("CLAIMS", claims.getStringClaim("client_id"));
        assertEquals(orgUuid, claims.getStringClaim("org"));
        assertThat(claims.getJWTID()).isNotBlank();
        assertThat(claims.getClaim("scope")).asString().contains("openid");
        assertEquals(Duration.ofMinutes(15),
                Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant()));
        assertThat(claims.getClaims()).doesNotContainKeys("email", "name", "roles", "password");
    }

    // CA-F1-05.2, RN-F1-05.3
    @Test
    void idTokenCarriesNonceAndProfile() throws Exception {
        JWTClaimsSet idToken = login("openid profile email", "nonce", "abc")[1];

        assertEquals("abc", idToken.getStringClaim("nonce"));
        assertEquals(userUuid, idToken.getSubject());
        assertThat(idToken.getAudience()).containsExactly("CLAIMS");
        assertEquals("Nome claims.user", idToken.getStringClaim("name"));
        assertEquals("claims.user", idToken.getStringClaim("preferred_username"));
        assertEquals("claims.user@sentry.dev", idToken.getStringClaim("email"));
        assertThat(idToken.getClaim("auth_time")).isNotNull();
        assertThat(idToken.getStringClaim("sid")).isNotBlank();
        assertEquals(Duration.ofMinutes(15),
                Duration.between(idToken.getIssueTime().toInstant(), idToken.getExpirationTime().toInstant()));
    }

    // CA-F1-05.3
    @Test
    void idTokenWithoutEmailScopeHasNoEmail() throws Exception {
        JWTClaimsSet idToken = login("openid")[1];

        assertThat(idToken.getClaims()).doesNotContainKeys("email", "name", "preferred_username");
    }

    // CA-F1-05.4: uma biblioteca JWT padrão recusa o token 16 minutos depois
    @Test
    void accessTokenExpiresAfterFifteenMinutes() throws Exception {
        String code = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("CLAIMS", "openid"), "claims.user", PASSWORD)
                .code();
        String token = PkceFlow.json(PkceFlow.exchange(mockMvc, "CLAIMS", null, code, CODE_VERIFIER), "$.access_token");
        RSAKey key = (RSAKey) JWKSet.parse(mockMvc.perform(get("/oauth2/jwks")).andReturn().getResponse()
                .getContentAsString()).getKeyByKeyId(SignedJWT.parse(token).getHeader().getKeyID());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
        var validator = new JwtTimestampValidator(Duration.ZERO);
        validator.setClock(Clock.fixed(Instant.now().plus(Duration.ofMinutes(16)), ZoneOffset.UTC));
        decoder.setJwtValidator(validator);

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    // RN-F1-06.6 (parte): usuário desativado entre o login e a troca não recebe token
    @Test
    void inactiveUserGetsNoToken() throws Exception {
        String code = PkceFlow.loginAndAuthorize(mockMvc, authorizeUrl("CLAIMS", "openid"), "claims.user", PASSWORD)
                .code();
        jdbcTemplate.update("UPDATE users SET is_active = FALSE WHERE username = 'claims.user'");
        entityManager.clear();

        PkceFlow.exchange(mockMvc, "CLAIMS", null, code, CODE_VERIFIER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }
}
