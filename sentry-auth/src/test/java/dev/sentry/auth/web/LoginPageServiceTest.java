package dev.sentry.auth.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.accesslog.AccessLog.Channel;
import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.api.SystemRepository;
import dev.sentry.auth.provider.LoginAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

class LoginPageServiceTest {

    private final SystemRepository systemRepository = mock(SystemRepository.class);
    private final LoginPageService service = new LoginPageService(systemRepository);

    // o Spring Security guarda o pedido de autorização na sessão antes de mandar para a tela
    private static MockHttpServletRequest afterAuthorizationRedirect() {
        var authorize = new MockHttpServletRequest("GET", "/oauth2/authorize");
        authorize.setParameter("client_id", "SYS001");
        new HttpSessionRequestCache().saveRequest(authorize, new MockHttpServletResponse());
        var page = new MockHttpServletRequest("GET", "/login");
        page.setSession(authorize.getSession());
        return page;
    }

    // RN-F1-04.2
    @Test
    void requestingSystemNameComesFromSavedAuthorization() {
        ExternalSystem system = new ExternalSystem();
        system.setDescription("Sistema de Exemplo");
        when(systemRepository.findByExternalCode("SYS001")).thenReturn(system);

        assertEquals("Sistema de Exemplo", service.requestingSystemName(afterAuthorizationRedirect()));
        assertNull(service.requestingSystemName(new MockHttpServletRequest("GET", "/login")));
    }

    // RN-F1-04.4
    @Test
    void failedUsernameIsShownOnce() {
        var post = new MockHttpServletRequest("POST", "/login");
        post.setParameter("username", "maria");
        service.rememberFailedUsername(post);
        var page = new MockHttpServletRequest("GET", "/login");
        page.setSession(post.getSession());

        assertEquals("maria", service.takeFailedUsername(page));
        assertNull(service.takeFailedUsername(page));
    }

    // F1-10: a tentativa pela tela leva o sistema do fluxo, o canal, o IP e o User-Agent
    @Test
    void loginAttemptCarriesFlowContext() {
        MockHttpServletRequest request = afterAuthorizationRedirect();
        request.setRemoteAddr("203.0.113.7");
        request.addHeader("User-Agent", "JUnit");

        assertEquals(new LoginAttempt("SYS001", Channel.PKCE, "203.0.113.7", "JUnit"), service.loginAttempt(request));
    }
}
