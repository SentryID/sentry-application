package dev.sentry.auth.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * RN-F1-09.7: com {@code prompt=login}, encerra a sessão SSO e repete a autorização sem o {@code prompt}, o que leva o
 * usuário à tela de login mesmo que já estivesse autenticado.
 */
public class PromptLoginFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String prompt = request.getParameter("prompt");
        return !"/oauth2/authorize".equals(request.getRequestURI()) || prompt == null
                || !prompt.matches("(^|.* )login( .*|$)");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            chain.doFilter(request, response);
            return;
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        String withoutPrompt = UriComponentsBuilder.fromUriString(request.getRequestURL() + "?" + request.getQueryString())
                .replaceQueryParam("prompt")
                .build(true)
                .toUriString();
        response.sendRedirect(withoutPrompt);
    }
}
