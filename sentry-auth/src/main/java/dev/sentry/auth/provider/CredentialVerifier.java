package dev.sentry.auth.provider;

import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.User.AuthenticationType;

/**
 * Confere a senha de um usuário de um {@link AuthenticationType} (RN-F1-02.7). O restante da tentativa (usuário
 * ativo, sistema, papel, registro em {@code access_logs}) é do {@link UserAuthenticationProvider}; para o LDAP
 * entrar basta uma nova implementação.
 */
public interface CredentialVerifier {

    /** @return o tipo de usuário que este verificador confere */
    AuthenticationType type();

    /**
     * Confere a senha sem lançar exceção de credencial.
     *
     * @param user usuário existente do {@link #type()}
     * @param password senha em claro
     * @return se conferiu e o que fazer caso a tentativa inteira tenha sucesso
     */
    Verification verify(User user, String password);

    /**
     * @param matches se a senha conferiu
     * @param onSuccess executado só quando a tentativa inteira tem sucesso (ex.: re-hash, RN-F0-02.4)
     */
    record Verification(boolean matches, Runnable onSuccess) {

        /** @return senha conferida, sem nada a fazer no sucesso */
        public static Verification accepted() {
            return new Verification(true, () -> { });
        }

        /** @return senha não conferida */
        public static Verification rejected() {
            return new Verification(false, () -> { });
        }
    }
}
