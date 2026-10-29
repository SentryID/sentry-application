package dev.sentry.auth.web;

import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.api.SystemRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Tela de login hospedada do fluxo PKCE (F1-04). O envio do formulário é tratado pelo Spring Security. */
@Controller
public class LoginPageController {

    /** Username da última tentativa que falhou, para a tela voltar com ele preenchido (RN-F1-04.4). */
    public static final String LAST_USERNAME = "SENTRY_LAST_USERNAME";

    private static final HttpSessionRequestCache REQUEST_CACHE = new HttpSessionRequestCache();

    private final SystemRepository systemRepository;

    public LoginPageController(SystemRepository systemRepository) {
        this.systemRepository = systemRepository;
    }

    @GetMapping("/login")
    String login(HttpServletRequest request, Model model) {
        String clientId = requestedClientId(request);
        ExternalSystem system = clientId == null ? null : systemRepository.findByExternalCode(clientId);
        // RN-F1-04.2: a tela mostra qual sistema pediu o login
        model.addAttribute("systemName", system == null ? null : system.getDescription());
        model.addAttribute("error", request.getParameter("error") != null);
        HttpSession session = request.getSession(false);
        if (session != null) {
            model.addAttribute("username", session.getAttribute(LAST_USERNAME));
            session.removeAttribute(LAST_USERNAME);
        }
        return "login";
    }

    /**
     * {@code client_id} do pedido de autorização que levou à tela de login, guardado na sessão pelo Spring Security.
     *
     * @param request requisição atual
     * @return o {@code client_id}, ou {@code null} quando a tela foi aberta sem um fluxo de autorização
     */
    public static String requestedClientId(HttpServletRequest request) {
        SavedRequest saved = REQUEST_CACHE.getRequest(request, null);
        String[] clientIds = saved == null ? null : saved.getParameterValues("client_id");
        return clientIds == null || clientIds.length == 0 ? null : clientIds[0];
    }
}
