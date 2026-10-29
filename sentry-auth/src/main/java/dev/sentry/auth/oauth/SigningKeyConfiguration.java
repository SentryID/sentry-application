package dev.sentry.auth.oauth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;

/**
 * Chaves RS256 dos tokens (F1-07). A chave atual assina; a anterior, quando configurada, continua no JWKS para que
 * tokens assinados antes da troca validem até expirar (RN-F1-07.5).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SigningKeyConfiguration.SigningKeyProperties.class)
public class SigningKeyConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SigningKeyConfiguration.class);

    /**
     * @param signingKey chave privada atual, PKCS#8 em PEM: o conteúdo ou o caminho ({@code file:/run/secrets/...})
     * @param previousKey chave anterior (privada PKCS#8 ou pública X.509, em PEM), só para validar tokens antigos
     * @param generateIfMissing gera uma chave temporária quando nenhuma é configurada; só para desenvolvimento
     */
    @ConfigurationProperties("sentry.jwt")
    public record SigningKeyProperties(String signingKey, String previousKey, boolean generateIfMissing) {
    }

    @Bean
    RSAKey currentSigningKey(SigningKeyProperties properties) {
        if (properties.signingKey() != null && !properties.signingKey().isBlank()) {
            return toJwk(readPem(properties.signingKey()));
        }
        if (!properties.generateIfMissing()) {
            // RN-F1-07.3 e CA-F1-07.4: em produção a chave vem de fora
            throw new IllegalStateException("Chave de assinatura dos tokens não configurada: defina SENTRY_JWT_SIGNING_KEY "
                    + "com a chave privada RSA em PEM (PKCS#8) ou, só em desenvolvimento, SENTRY_JWT_GENERATE_IF_MISSING=true");
        }
        log.warn("Usando uma chave de assinatura temporária, gerada nesta subida: os tokens deixam de validar quando o "
                + "serviço reinicia. Não use em produção; configure SENTRY_JWT_SIGNING_KEY (RN-F1-07.4)");
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return toJwk(generator.generateKeyPair().getPrivate().getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(RSAKey currentSigningKey, SigningKeyProperties properties) {
        List<JWK> keys = new ArrayList<>(List.of(currentSigningKey));
        if (properties.previousKey() != null && !properties.previousKey().isBlank()) {
            keys.add(toJwk(readPem(properties.previousKey())).toPublicJWK());
        }
        return new ImmutableJWKSet<>(new JWKSet(keys));
    }

    // só a chave atual assina; com as duas no mesmo conjunto o encoder não saberia qual usar
    @Bean
    JwtEncoder jwtEncoder(RSAKey currentSigningKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(currentSigningKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    private static byte[] readPem(String value) {
        String pem = value.strip();
        if (!pem.startsWith("-----BEGIN")) {
            try (var in = new DefaultResourceLoader().getResource(pem).getInputStream()) {
                pem = new String(in.readAllBytes(), StandardCharsets.US_ASCII);
            } catch (IOException e) {
                throw new IllegalStateException("Não foi possível ler a chave de assinatura em " + value, e);
            }
        }
        String base64 = pem.replaceAll("-----(BEGIN|END) [A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    // aceita PKCS#8 (privada) ou X.509 (pública); o kid é o thumbprint da chave (RFC 7638)
    private static RSAKey toJwk(byte[] der) {
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAKey.Builder builder;
            try {
                var privateKey = (RSAPrivateCrtKey) factory.generatePrivate(new PKCS8EncodedKeySpec(der));
                var publicKey = (RSAPublicKey) factory.generatePublic(
                        new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
                builder = new RSAKey.Builder(publicKey).privateKey(privateKey);
            } catch (GeneralSecurityException notPrivate) {
                builder = new RSAKey.Builder((RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(der)));
            }
            return builder.keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.RS256).keyIDFromThumbprint().build();
        } catch (GeneralSecurityException | JOSEException | ClassCastException e) {
            throw new IllegalStateException("Chave de assinatura inválida: use RSA em PEM (PKCS#8 ou X.509)", e);
        }
    }
}
