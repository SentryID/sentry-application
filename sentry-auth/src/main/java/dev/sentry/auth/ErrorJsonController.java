package dev.sentry.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Substitui a página de erro do Spring Boot: todo erro encaminhado pelo contêiner sai em JSON. */
@RestController
public class ErrorJsonController implements ErrorController {

    private final ErrorDetailsService errorDetailsService;

    public ErrorJsonController(ErrorDetailsService errorDetailsService) {
        this.errorDetailsService = errorDetailsService;
    }

    @RequestMapping("/error")
    ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
        ProblemDetail problem = errorDetailsService.problem(request);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
