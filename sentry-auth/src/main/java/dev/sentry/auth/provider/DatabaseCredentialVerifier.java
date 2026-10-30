package dev.sentry.auth.provider;

import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.User.AuthenticationType;
import dev.sentry.api.domain.user.api.UserRepository;
import dev.sentry.api.security.password.PasswordService;
import org.springframework.stereotype.Component;

/** Confere a senha de usuários {@code DATABASE} contra o hash Argon2id gravado (F1-02). */
@Component
public class DatabaseCredentialVerifier implements CredentialVerifier {

    private final PasswordService passwordService;
    private final UserRepository userRepository;

    public DatabaseCredentialVerifier(PasswordService passwordService, UserRepository userRepository) {
        this.passwordService = passwordService;
        this.userRepository = userRepository;
    }

    @Override
    public AuthenticationType type() {
        return AuthenticationType.DATABASE;
    }

    /**
     * Confere a senha contra o hash do usuário. Quando os parâmetros do Argon2id ficaram mais fortes, o novo hash é
     * gravado só se a tentativa inteira tiver sucesso (RN-F0-02.4).
     *
     * @param user usuário {@code DATABASE}
     * @param password senha em claro
     * @return se conferiu e, no sucesso, a gravação do novo hash quando houver
     */
    @Override
    public Verification verify(User user, String password) {
        var verification = passwordService.verify(password, user.getPassword());
        if (!verification.matches()) {
            return Verification.rejected();
        }
        String upgradedHash = verification.upgradedHash();
        if (upgradedHash == null) {
            return Verification.accepted();
        }
        return new Verification(true, () -> {
            user.setPassword(upgradedHash);
            userRepository.save(user);
        });
    }
}
