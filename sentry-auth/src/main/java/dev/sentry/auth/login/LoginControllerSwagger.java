package dev.sentry.auth.login;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Login direto", description = "Autenticação por API para sistemas confiáveis (F1-11)")
@SecurityScheme(name = "clientBasic", type = SecuritySchemeType.HTTP, scheme = "basic",
        description = "client_id e client_secret do sistema confidencial e confiável")
@RequestMapping("/api/v1/auth")
public interface LoginControllerSwagger {

    @Operation(summary = "Autentica um usuário por usuário e senha",
            description = "Só para sistemas CONFIDENTIAL marcados como confiáveis, autenticados por HTTP Basic. Devolve os mesmos "
                    + "tokens do fluxo PKCE e não cria sessão SSO. Aceita apenas HTTPS fora de localhost.",
            security = @SecurityRequirement(name = "clientBasic"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário autenticado"),
            @ApiResponse(responseCode = "400", description = "Escopo não permitido ou requisição sem HTTPS",
                    content = @Content(schema = @Schema(implementation = LoginRest.ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Sistema não autenticado (invalid_client) ou usuário ou senha inválidos (invalid_grant)",
                    content = @Content(schema = @Schema(implementation = LoginRest.ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sistema não é confiável (unauthorized_client)",
                    content = @Content(schema = @Schema(implementation = LoginRest.ErrorResponse.class)))
    })
    @PostMapping("/login")
    ResponseEntity<LoginRest.TokenResponse> login(
            @RequestBody @Valid LoginRest.LoginRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest);
}
