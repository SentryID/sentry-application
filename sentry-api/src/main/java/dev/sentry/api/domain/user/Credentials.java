package dev.sentry.api.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash da senha, salt e tipo de autenticação — os três andam juntos e por isso moram no
 * mesmo value object. A senha em claro nunca é campo: entra em {@link #hash} e sai como
 * derivação PBKDF2.
 */
@Embeddable
public record Credentials(
        @Column(name = "password") String passwordHash,
        @Column(name = "salt") String salt,
        @Column(name = "authentication_type") String authenticationType) {

    // ponytail: iterações e algoritmo fixos aqui; subir o número junto com o hardware,
    // ou trocar por Argon2id quando o sentry-auth trouxer uma dependência de cripto.
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 210_000;
    private static final int TAMANHO_CHAVE_BITS = 256;
    private static final int TAMANHO_SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Deriva a senha com um salt novo. Único caminho de entrada de senha no domínio. */
    public static Credentials hash(String senhaEmClaro, String authenticationType) {
        if (senhaEmClaro == null || senhaEmClaro.isBlank()) {
            throw new IllegalArgumentException("Senha é obrigatória");
        }
        byte[] salt = new byte[TAMANHO_SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derivada = derivar(senhaEmClaro, salt);
        return new Credentials(
                Base64.getEncoder().encodeToString(derivada),
                Base64.getEncoder().encodeToString(salt),
                authenticationType);
    }

    /** Comparação em tempo constante — não vaza quantos bytes bateram. */
    public boolean matches(String senhaEmClaro) {
        if (passwordHash == null || salt == null || senhaEmClaro == null) {
            return false;
        }
        byte[] candidata = derivar(senhaEmClaro, Base64.getDecoder().decode(salt));
        return MessageDigest.isEqual(candidata, Base64.getDecoder().decode(passwordHash));
    }

    /** Troca só o tipo de autenticação, preservando hash e salt. */
    public Credentials withAuthenticationType(String authenticationType) {
        return new Credentials(passwordHash, salt, authenticationType);
    }

    private static byte[] derivar(String senhaEmClaro, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(senhaEmClaro.toCharArray(), salt, ITERACOES, TAMANHO_CHAVE_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao derivar a senha", e);
        } finally {
            spec.clearPassword();
        }
    }
}
