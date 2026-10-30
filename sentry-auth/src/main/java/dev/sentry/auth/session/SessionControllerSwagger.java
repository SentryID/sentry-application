package dev.sentry.auth.session;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Sessão", description = "Sessão SSO para a tela de login do front-end, na mesma origem do sentry-auth. "
        + "Os POST exigem o cabeçalho X-XSRF-TOKEN com o valor do cookie XSRF-TOKEN")
@RequestMapping("/api/v1/session")
public interface SessionControllerSwagger {

    @Operation(summary = "Contexto da tela de login", description = "Sistema que pediu a autenticação, quando há um fluxo em andamento")
    @GetMapping("/context")
    SessionRest.Context context(@Parameter(hidden = true) HttpServletRequest request);

    @Operation(summary = "Faz login e cria a sessão SSO",
            description = "Devolve a URL que retoma a autorização; o navegador deve abri-la")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão criada"),
            @ApiResponse(responseCode = "401", description = "Usuário ou senha inválidos"),
            @ApiResponse(responseCode = "403", description = "Token CSRF ausente ou inválido")
    })
    @PostMapping("/login")
    SessionRest.LoginResponse login(@RequestBody @Valid SessionRest.LoginRequest login,
            @Parameter(hidden = true) HttpServletRequest request, @Parameter(hidden = true) HttpServletResponse response);

    @Operation(summary = "Usuário da sessão SSO atual")
    @GetMapping("/me")
    SessionRest.Me me();

    @Operation(summary = "Encerra a sessão SSO")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Parameter(hidden = true) HttpServletRequest request);
}
