package dev.sentry.api.domain.shared;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Gera o valor de um token. Fica em um lugar só de propósito: token de redefinição de
 * senha e de MFA nunca podem vir do cliente.
 */
public final class SecureToken {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TAMANHO_BYTES = 32;

    private SecureToken() {
    }

    public static String generate() {
        byte[] bytes = new byte[TAMANHO_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
