package dev.sentry.auth.login;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/** Contratos REST do login: os records de request/response dos endpoints. */
public interface LoginRest {

    @Schema(description = "Requisição de login")
    record LoginRequest(
            @Schema(description = "Nome de usuário", example = "john.doe", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String username,
            @Schema(description = "Senha do usuário", example = "secret", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String password,
            @Schema(description = "Sistema no qual o usuário está se autenticando", example = "SYS001",
                    requiredMode = RequiredMode.REQUIRED)
            @NotBlank String system) {
    }

    @Schema(description = "Resposta de login")
    record LoginResponse(
            @Schema(description = "Identificador público do usuário", example = "3f2504e0-4f89-11d3-9a0c-0305e82c3301")
            UUID userId,
            @Schema(description = "Nome de usuário", example = "john.doe")
            String username,
            @Schema(description = "Token do usuário")
            String userToken,
            @Schema(description = "Token da aplicação")
            String applicationToken) {
    }
}
