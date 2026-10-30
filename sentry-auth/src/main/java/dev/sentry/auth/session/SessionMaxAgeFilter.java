package dev.sentry.auth.session;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RN-F1-09.2: encerra a sessão SSO que passou da vida máxima, mesmo que tenha sido usada há pouco. Roda depois do
 * Spring Session e antes do Spring Security, que então vê a requisição sem sessão.
 */
@Component
// logo antes do filtro do Spring Security, que fica na ordem -100
@Order(-101)
public class SessionMaxAgeFilter extends OncePerRequestFilter {

    private final Duration maxAge;

    public SessionMaxAgeFilter(@Value("${sentry.session.max-age}") Duration maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && System.currentTimeMillis() - session.getCreationTime() > maxAge.toMillis()) {
            session.invalidate();
        }
        chain.doFilter(request, response);
    }
}
