package dev.sentry.auth.errors;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;

/**
 * Corpo JSON (Problem Details, RFC 9457) dos erros que o contêiner encaminha para {@code /error}, como os do
 * {@code /oauth2/authorize} com {@code client_id} ou {@code redirect_uri} inválidos (RN-F1-03.3): o sentry-auth não
 * devolve HTML.
 */
@Service
public class ErrorDetailsService {

    private static final Pattern OAUTH_PARAMETER = Pattern.compile("Parameter: (\\w+)");

    /**
     * Monta o erro a partir dos atributos que o contêiner grava na requisição encaminhada.
     *
     * @param request requisição despachada para {@code /error}
     * @return o status original e uma mensagem em português; nos erros OAuth, com o parâmetro recusado
     */
    public ProblemDetail problem(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = HttpStatus.resolve(code instanceof Integer value ? value : 500);
        status = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Matcher parameter = OAUTH_PARAMETER.matcher(message == null ? "" : message.toString());
        String detail = switch (status) {
            case BAD_REQUEST -> parameter.find()
                    ? "Pedido de autorização inválido: parâmetro " + parameter.group(1)
                    : "Pedido inválido";
            case UNAUTHORIZED -> "Autenticação necessária";
            case FORBIDDEN -> "Acesso negado";
            case NOT_FOUND -> "Recurso não encontrado";
            default -> "Erro ao processar o pedido";
        };
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
