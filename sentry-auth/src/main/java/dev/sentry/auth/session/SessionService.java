package dev.sentry.auth.session;

import dev.sentry.api.domain.accesslog.AccessLog.Channel;
import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.api.SystemRepository;
import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.api.UserRepository;
import dev.sentry.auth.provider.DatabaseAuthenticationProvider;
import dev.sentry.auth.provider.LoginAttempt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sessão SSO do sentry-auth sem telas próprias (F1-04, F1-09): a tela de login é do front-end, que chama esta API na
 * mesma origem. O pedido de autorização interrompido fica guardado na sessão e é retomado depois do login.
 */
@Service
public class SessionService {

    private static final HttpSessionRequestCache REQUEST_CACHE = new HttpSessionRequestCache();

    private final ProviderManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SystemRepository systemRepository;
    private final UserRepository userRepository;

    public SessionService(List<AuthenticationProvider> authenticationProviders, SystemRepository systemRepository,
            UserRepository userRepository) {
        // os mesmos provedores do login direto: o LDAP, quando entrar, vale aqui sem mudar este código (RN-F1-02.7)
        this.authenticationManager = new ProviderManager(authenticationProviders);
        this.systemRepository = systemRepository;
        this.userRepository = userRepository;
    }

    /**
     * Qual sistema pediu o login, para a tela exibir (RN-F1-04.2).
     *
     * @param request requisição com o cookie da sessão
     * @return o {@code client_id} e o nome do sistema, ou campos nulos quando não há fluxo em andamento
     */
    public SessionRest.Context context(HttpServletRequest request) {
        String clientId = requestedClientId(request);
        ExternalSystem system = clientId == null ? null : systemRepository.findByExternalCode(clientId);
        return new SessionRest.Context(system == null ? null : clientId, system == null ? null : system.getDescription());
    }

    /**
     * Autentica o usuário no sistema do fluxo em andamento e cria a sessão SSO. A tentativa é registrada com o canal
     * PKCE (F1-10), o id da sessão é renovado (RN-F1-09.3) e a resposta traz a URL que retoma a autorização.
     *
     * @param login credenciais digitadas na tela
     * @param request requisição com o cookie da sessão
     * @param response resposta, onde a sessão autenticada é gravada
     * @return a URL do {@code /oauth2/authorize} a abrir, ou nula sem fluxo em andamento
     * @throws ResponseStatusException {@code 401} com a mensagem genérica em qualquer falha (RN-F1-02.3)
     */
    public SessionRest.LoginResponse login(SessionRest.LoginRequest login, HttpServletRequest request,
            HttpServletResponse response) {
        var attempt = UsernamePasswordAuthenticationToken.unauthenticated(login.username(), login.password());
        attempt.setDetails(new LoginAttempt(requestedClientId(request), Channel.PKCE, request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT)));
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(attempt);
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    DatabaseAuthenticationProvider.INVALID_CREDENTIALS_MESSAGE);
        }
        request.getSession();
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        SavedRequest saved = REQUEST_CACHE.getRequest(request, response);
        return new SessionRest.LoginResponse(saved == null ? null : saved.getRedirectUrl());
    }

    /**
     * Usuário da sessão SSO atual, para o front-end saber se já há login.
     *
     * @return os dados do usuário, ou {@code authenticated=false} sem sessão
     */
    public SessionRest.Me me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SessionRest.Me.ANONYMOUS;
        }
        User user = userRepository.findByUuid(UUID.fromString(authentication.getName()));
        return user == null ? SessionRest.Me.ANONYMOUS
                : new SessionRest.Me(true, authentication.getName(), user.getUsername(), user.getName());
    }

    /**
     * Encerra a sessão SSO. Os refresh tokens dos sistemas continuam válidos; para revogá-los, o sistema usa o
     * {@code /connect/logout} com o {@code id_token_hint} (RN-F1-09.6).
     *
     * @param request requisição com o cookie da sessão
     */
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    // client_id do pedido de autorização que levou à tela, guardado na sessão pelo Spring Security
    private static String requestedClientId(HttpServletRequest request) {
        SavedRequest saved = REQUEST_CACHE.getRequest(request, null);
        String[] clientIds = saved == null ? null : saved.getParameterValues("client_id");
        return clientIds == null || clientIds.length == 0 ? null : clientIds[0];
    }
}
