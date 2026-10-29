package dev.sentry.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import dev.sentry.auth.oauth.SigningKeyConfiguration.SigningKeyProperties;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class SigningKeyConfigurationTest {

    private final SigningKeyConfiguration configuration = new SigningKeyConfiguration();

    private static String newPem() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String base64 = Base64.getMimeEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
    }

    // CA-F1-07.4
    @Test
    void refusesToStartWithoutKeyOutsideDevelopment() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> configuration.currentSigningKey(new SigningKeyProperties(null, null, false)));

        assertThat(exception.getMessage()).contains("SENTRY_JWT_SIGNING_KEY");
    }

    // RN-F1-07.3
    @Test
    void loadsConfiguredPemWithThumbprintKid() throws Exception {
        RSAKey key = configuration.currentSigningKey(new SigningKeyProperties(newPem(), null, false));

        assertThat(key.isPrivate()).isTrue();
        assertEquals(key.computeThumbprint().toString(), key.getKeyID());
    }

    // CA-F1-07.3, RN-F1-07.5
    @Test
    void tokenSignedWithPreviousKeyStillValidatesAfterRotation() throws Exception {
        String previousPem = newPem();
        RSAKey previous = configuration.currentSigningKey(new SigningKeyProperties(previousPem, null, false));
        var properties = new SigningKeyProperties(newPem(), previousPem, false);
        RSAKey current = configuration.currentSigningKey(properties);
        JWKSource<SecurityContext> jwks = configuration.jwkSource(current, properties);

        String oldToken = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(previous)))
                .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId(previous.getKeyID()).build(),
                        JwtClaimsSet.builder().subject("u").expiresAt(Instant.now().plusSeconds(60)).build()))
                .getTokenValue();

        assertEquals("u", configuration.jwtDecoder(jwks).decode(oldToken).getSubject());
        assertThat(((ImmutableJWKSet<SecurityContext>) jwks).getJWKSet().getKeys()).hasSize(2)
                .allSatisfy(jwk -> assertThat(jwk.getKeyID()).isNotBlank());
    }
}
