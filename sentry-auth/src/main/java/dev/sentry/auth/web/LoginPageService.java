package dev.sentry.auth.web;

import dev.sentry.api.domain.accesslog.AccessLog.Channel;
import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.api.SystemRepository;
import dev.sentry.auth.provider.LoginAttempt;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Service;

/** Regras da tela de login hospedada (F1-04): qual sistema pediu o login e o username da última falha. */
@Service
public class LoginPageService {

    private static final String LAST_USERNAME = "SENTRY_LAST_USERNAME";
    private static final HttpSessionRequestCache REQUEST_CACHE = new HttpSessionRequestCache();

    private final SystemRepository systemRepository;

    public LoginPageService(SystemRepository systemRepository) {
        this.systemRepository = systemRepository;
    }

    /**
     * Nome do sistema que pediu o login, para a tela mostrar (RN-F1-04.2).
     *
     * @param request requisição da tela
     * @return a descrição do sistema, ou {@code null} quando a tela foi aberta sem um fluxo de autorização
     */
    public String requestingSystemName(HttpServletRequest request) {
        String clientId = requestedClientId(request);
        ExternalSystem system = clientId == null ? null : systemRepository.findByExternalCode(clientId);
        return system == null ? null : system.getDescription();
    }

    /**
     * Guarda o username de uma tentativa que falhou, para a tela voltar com ele preenchido (RN-F1-04.4).
     *
     * @param request requisição do envio do formulário
     */
    public void rememberFailedUsername(HttpServletRequest request) {
        request.getSession().setAttribute(LAST_USERNAME, request.getParameter("username"));
    }

    /**
     * Devolve e apaga o username guardado por {@link #rememberFailedUsername}; a senha nunca é guardada.
     *
     * @param request requisição da tela
     * @return o username, ou {@code null}
     */
    public String takeFailedUsername(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object username = session.getAttribute(LAST_USERNAME);
        session.removeAttribute(LAST_USERNAME);
        return (String) username;
    }

    /**
     * Contexto da tentativa enviada pela tela: o sistema do fluxo, o canal PKCE, o IP e o {@code User-Agent} (F1-10).
     *
     * @param request requisição do envio do formulário
     * @return os detalhes levados ao provedor de autenticação
     */
    public LoginAttempt loginAttempt(HttpServletRequest request) {
        return new LoginAttempt(requestedClientId(request), Channel.PKCE, request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT));
    }

    // client_id do pedido de autorização que levou à tela, guardado na sessão pelo Spring Security
    private static String requestedClientId(HttpServletRequest request) {
        SavedRequest saved = REQUEST_CACHE.getRequest(request, null);
        String[] clientIds = saved == null ? null : saved.getParameterValues("client_id");
        return clientIds == null || clientIds.length == 0 ? null : clientIds[0];
    }
}
