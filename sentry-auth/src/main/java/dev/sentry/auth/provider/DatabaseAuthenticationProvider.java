package dev.sentry.auth.provider;

import dev.sentry.api.domain.access.roleuser.api.RoleUserRepository;
import dev.sentry.api.domain.accesslog.AccessLog;
import dev.sentry.api.domain.accesslog.api.AccessLogService;
import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.api.SystemRepository;
import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.api.UserRepository;
import dev.sentry.api.security.password.PasswordService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valida usuário e senha no banco (F1-02). É o ponto único usado pela tela de login (F1-04) e pelo login
 * direto (F1-11); o sistema que pediu a autenticação vem nos {@code details}, como {@link LoginAttempt}.
 */
@Component
public class DatabaseAuthenticationProvider implements AuthenticationProvider {

    /** RN-F1-02.3: a mesma mensagem para qualquer falha. */
    public static final String INVALID_CREDENTIALS_MESSAGE = "Usuário ou senha inválidos";
    static final String AUTHENTICATION_TYPE = "DATABASE";

    private final UserRepository userRepository;
    private final SystemRepository systemRepository;
    private final RoleUserRepository roleUserRepository;
    private final PasswordService passwordService;
    private final AccessLogService accessLogService;
    // RN-F1-02.4: hash de verdade, para que usuário inexistente custe o mesmo tempo que senha errada
    private final String dummyHash;

    public DatabaseAuthenticationProvider(UserRepository userRepository, SystemRepository systemRepository,
            RoleUserRepository roleUserRepository, PasswordService passwordService,
            AccessLogService accessLogService) {
        this.userRepository = userRepository;
        this.systemRepository = systemRepository;
        this.roleUserRepository = roleUserRepository;
        this.passwordService = passwordService;
        this.accessLogService = accessLogService;
        this.dummyHash = passwordService.hash(UUID.randomUUID().toString());
    }

    /**
     * Autentica o usuário no sistema informado nos {@code details}. Em sucesso, o principal é o {@code uuid} do
     * usuário, que vira o {@code sub} dos tokens (RN-F1-05.4). Toda tentativa é registrada em {@code access_logs}
     * com o motivo real (RN-F1-02.6, RN-F1-10.1).
     *
     * @param authentication username, senha e {@link LoginAttempt}
     * @return autenticação com o {@code uuid} do usuário como nome
     * @throws BadCredentialsException com a mensagem genérica em qualquer falha (RN-F1-02.3)
     */
    @Override
    @Transactional
    public Authentication authenticate(Authentication authentication) {
        LoginAttempt attempt = (LoginAttempt) authentication.getDetails();
        Outcome outcome = evaluate(authentication.getName(), (String) authentication.getCredentials(),
                attempt.clientId());
        accessLogService.recordAttempt(toAccessLog(authentication.getName(), attempt, outcome));
        if (outcome.result() != AttemptResult.SUCCESS) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
        // o fator senha leva o instante do login, que vira o auth_time do ID token (RN-F1-05.3)
        var authenticated = UsernamePasswordAuthenticationToken.authenticated(outcome.user().getUuid().toString(), null,
                List.of(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY)));
        // a autenticação é gravada pelo Authorization Server em JSON, que só aceita tipos conhecidos; sem isto o
        // ProviderManager copiaria o LoginAttempt para cá
        authenticated.setDetails(new WebAuthenticationDetails(attempt.ipAddress(), null));
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * Decide o resultado da tentativa sem lançar exceção. A senha é sempre verificada contra um hash, mesmo
     * quando o usuário não existe (RN-F1-02.4). Em sucesso, grava o novo hash se os parâmetros do Argon2id
     * ficaram mais fortes (RN-F0-02.4).
     *
     * @param username login digitado, sem diferenciar maiúsculas (RN-F1-02.1)
     * @param password senha em claro
     * @param clientId {@code client_id} do sistema que pediu a autenticação
     * @return o resultado, o usuário (quando existe) e o sistema (quando existe)
     */
    Outcome evaluate(String username, String password, String clientId) {
        ExternalSystem system = clientId == null ? null : systemRepository.findByExternalCode(clientId);
        User user = username == null ? null : userRepository.findByUsernameIgnoreCase(username);
        var verification = passwordService.verify(password, user != null ? user.getPassword() : dummyHash);

        AttemptResult result;
        if (user == null) {
            result = AttemptResult.USER_NOT_FOUND;
        } else if (!user.getIsActive() || user.getIsDeleted()) {
            result = AttemptResult.USER_INACTIVE;
        } else if (!AUTHENTICATION_TYPE.equals(user.getAuthenticationType()) || !verification.matches()) {
            // ponytail: só o provedor de banco existe; quando o LDAP entrar, usuários de outro tipo passam a
            // devolver null aqui para o ProviderManager tentar o próximo provedor (RN-F1-02.7)
            result = AttemptResult.INVALID_CREDENTIALS;
        } else if (!isAvailable(system)) {
            result = AttemptResult.SYSTEM_INACTIVE;
        } else if (!roleUserRepository.hasActiveRoleInSystem(user.getId(), system.getId())) {
            result = AttemptResult.NO_SYSTEM_ACCESS;
        } else {
            result = AttemptResult.SUCCESS;
            if (verification.upgradedHash() != null) {
                user.setPassword(verification.upgradedHash());
                userRepository.save(user);
            }
        }
        return new Outcome(result, user, system);
    }

    private static AccessLog toAccessLog(String username, LoginAttempt attempt, Outcome outcome) {
        AccessLog accessLog = new AccessLog();
        accessLog.setUser(outcome.user());
        accessLog.setSystem(outcome.system());
        accessLog.setAttemptedUsername(username);
        accessLog.setAttemptResult(outcome.result().name());
        accessLog.setChannel(attempt.channel());
        accessLog.setIpHost(attempt.ipAddress());
        accessLog.setUserAgent(attempt.userAgent());
        return accessLog;
    }

    // RN-F1-01.8
    private static boolean isAvailable(ExternalSystem system) {
        return system != null && system.getIsActive() && !system.getIsDeleted()
                && system.getOrganization().getIsActive() && !system.getOrganization().getIsDeleted();
    }

    /** Valores de {@code access_logs.attempt_result} (RN-F1-10.2). */
    public enum AttemptResult {
        SUCCESS, INVALID_CREDENTIALS, USER_NOT_FOUND, USER_INACTIVE, NO_SYSTEM_ACCESS, SYSTEM_INACTIVE
    }

    record Outcome(AttemptResult result, User user, ExternalSystem system) {
    }
}
