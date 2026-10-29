package dev.sentry.auth.direct;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DirectLoginController implements DirectLoginControllerSwagger {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    private final DirectLoginService directLoginService;

    public DirectLoginController(DirectLoginService directLoginService) {
        this.directLoginService = directLoginService;
    }

    @Override
    public ResponseEntity<DirectLoginRest.TokenResponse> login(String authorization, DirectLoginRest.LoginRequest request,
            HttpServletRequest httpRequest) {
        // RN-F1-11.10: a senha só trafega em HTTPS, salvo em ambiente local
        if (!httpRequest.isSecure() && !LOCAL_HOSTS.contains(httpRequest.getServerName())) {
            throw new DirectLoginException(HttpStatus.BAD_REQUEST, "invalid_request", "Use HTTPS");
        }
        String[] client = basicCredentials(authorization);
        var tokens = directLoginService.login(client[0], client[1], request, httpRequest.getRemoteAddr(),
                httpRequest.getHeader(HttpHeaders.USER_AGENT));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(tokens);
    }

    @ExceptionHandler(DirectLoginException.class)
    ResponseEntity<DirectLoginRest.ErrorResponse> handle(DirectLoginException exception) {
        var response = ResponseEntity.status(exception.getStatus()).cacheControl(CacheControl.noStore());
        if ("invalid_client".equals(exception.getError())) {
            response.header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"sentry\"");
        }
        return response.body(new DirectLoginRest.ErrorResponse(exception.getError(), exception.getMessage()));
    }

    // HTTP Basic com client_id e client_secret codificados como no RFC 6749, 2.3.1
    private static String[] basicCredentials(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            return new String[] {null, null};
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(authorization.substring(6).trim()), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                return new String[] {null, null};
            }
            return new String[] {URLDecoder.decode(decoded.substring(0, colon), StandardCharsets.UTF_8),
                    URLDecoder.decode(decoded.substring(colon + 1), StandardCharsets.UTF_8)};
        } catch (IllegalArgumentException e) {
            return new String[] {null, null};
        }
    }
}
