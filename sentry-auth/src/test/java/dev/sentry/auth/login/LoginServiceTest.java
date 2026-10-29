package dev.sentry.auth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import dev.sentry.api.domain.system.api.SystemRepository;
import dev.sentry.api.security.password.PasswordService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

class LoginServiceTest {

    private final RegisteredClientRepository clients = mock(RegisteredClientRepository.class);
    @SuppressWarnings("unchecked")
    private final LoginService loginService = new LoginService(clients, mock(SystemRepository.class),
            mock(PasswordService.class), List.of(mock(AuthenticationProvider.class)), mock(OAuth2TokenGenerator.class),
            mock(OAuth2AuthorizationService.class), AuthorizationServerSettings.builder().build());

    private static final LoginRest.LoginRequest REQUEST = new LoginRest.LoginRequest("maria", "S3nha!forte", null);

    private static String basic(String credentials) {
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    // RN-F1-11.10
    @Test
    void plainHttpOutsideLocalhostIsInvalidRequest() {
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("sentry.exemplo.com");

        LoginException exception = assertThrows(LoginException.class, () -> loginService.login(REQUEST, request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("invalid_request", exception.getError());
        verifyNoInteractions(clients);
    }

    @Test
    void httpsOutsideLocalhostReachesClientAuthentication() {
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setServerName("sentry.exemplo.com");
        request.setSecure(true);
        request.addHeader("Authorization", basic("PORTAL:segredo"));

        LoginException exception = assertThrows(LoginException.class, () -> loginService.login(REQUEST, request));

        assertEquals("invalid_client", exception.getError());
        verify(clients).findByClientId("PORTAL");
    }

    // RN-F1-11.2: sem HTTP Basic válido, o sistema não autentica
    @Test
    void missingOrMalformedBasicIsInvalidClient() {
        for (String header : new String[] {null, "Bearer abc", "Basic !!!", basic("sem-dois-pontos")}) {
            var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            if (header != null) {
                request.addHeader("Authorization", header);
            }

            LoginException exception = assertThrows(LoginException.class, () -> loginService.login(REQUEST, request));

            assertThat(exception.getError()).as(String.valueOf(header)).isEqualTo("invalid_client");
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        }
    }

    // RFC 6749, 2.3.1: client_id e segredo vêm codificados como formulário
    @Test
    void basicCredentialsAreUrlDecoded() {
        assertArrayEquals(new String[] {"MEU SISTEMA", "a:b+c"},
                LoginService.basicCredentials(basic("MEU+SISTEMA:a%3Ab%2Bc")));
    }
}
