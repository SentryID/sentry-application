package dev.sentry.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

class ErrorDetailsServiceTest {

    private final ErrorDetailsService service = new ErrorDetailsService();

    private ProblemDetail problem(Integer status, String message) {
        var request = new MockHttpServletRequest("GET", "/error");
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, message);
        return service.problem(request);
    }

    // RN-F1-03.3: client_id ou redirect_uri inválido vira JSON, não página
    @Test
    void oauthErrorNamesTheRejectedParameter() {
        ProblemDetail problem = problem(400, "[invalid_request] OAuth 2.0 Parameter: redirect_uri");

        assertEquals(400, problem.getStatus());
        assertEquals("Pedido de autorização inválido: parâmetro redirect_uri", problem.getDetail());
    }

    @Test
    void otherErrorsGetGenericMessages() {
        assertEquals("Pedido inválido", problem(400, null).getDetail());
        assertEquals("Recurso não encontrado", problem(404, "Not Found").getDetail());
        assertEquals(500, problem(null, null).getStatus());
    }
}
