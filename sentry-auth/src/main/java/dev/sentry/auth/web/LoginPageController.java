package dev.sentry.auth.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Tela de login hospedada do fluxo PKCE (F1-04). O envio do formulário é tratado pelo Spring Security. */
@Controller
public class LoginPageController {

    private final LoginPageService loginPageService;

    public LoginPageController(LoginPageService loginPageService) {
        this.loginPageService = loginPageService;
    }

    @GetMapping("/login")
    String login(HttpServletRequest request, Model model) {
        model.addAttribute("systemName", loginPageService.requestingSystemName(request));
        model.addAttribute("error", request.getParameter("error") != null);
        model.addAttribute("username", loginPageService.takeFailedUsername(request));
        return "login";
    }

    @GetMapping(SsoLogout.LOGGED_OUT_PATH)
    String loggedOut() {
        return "logout";
    }
}
