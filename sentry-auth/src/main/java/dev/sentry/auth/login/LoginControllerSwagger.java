package dev.sentry.auth.login;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Login", description = "Autenticação de usuários")
@RequestMapping("/login")
public interface LoginControllerSwagger {

    @Operation(summary = "Autentica um usuário", description = "Valida as credenciais e retorna os tokens de usuário e de aplicação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário autenticado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    LoginRest.LoginResponse login(@RequestBody @Valid LoginRest.LoginRequest request);
}
