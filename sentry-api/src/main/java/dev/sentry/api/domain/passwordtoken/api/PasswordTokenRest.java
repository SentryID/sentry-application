package dev.sentry.api.domain.passwordtoken.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Contratos REST do token de senha: os records de request/response dos endpoints. */
public interface PasswordTokenRest {

    @Schema(description = "Requisição para consulta de tokens de senha")
    record QueryRequest(
            @Schema(description = "Token de redefinição de senha", example = "b7a1c3d4e5f6")
            String token,
            @Schema(description = "Identificador interno do usuário", example = "1")
            Long idUser,
            @Schema(description = "Indica se o token já foi utilizado", example = "false")
            Boolean isUsed,
            @Schema(description = "Número da página para paginação", example = "0", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer page,
            @Schema(description = "Tamanho da página para paginação", example = "10", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer pageSize,
            @Schema(description = "Campo para ordenação dos resultados.", example = "id,asc;createdAt,desc")
            String sortBy) {
    }

    @Schema(description = "Dados de um token de senha")
    record Response(
            @Schema(description = "Identificador interno do token de senha", example = "1")
            Long id,
            @Schema(description = "Token de redefinição de senha", example = "b7a1c3d4e5f6")
            String token,
            @Schema(description = "Indica se o token já foi utilizado", example = "false")
            Boolean isUsed,
            @Schema(description = "Identificador interno do usuário", example = "1")
            Long idUser,
            @Schema(description = "Data e hora limite de validade do token", example = "2026-02-20T15:05:00")
            LocalDateTime validUntil,
            @Schema(description = "Usuário que criou o registro", example = "system")
            String createdBy,
            @Schema(description = "Data e hora de criação do registro", example = "2026-02-20T14:05:00")
            LocalDateTime createdAt,
            @Schema(description = "Usuário que realizou a última alteração", example = "system")
            String updatedBy,
            @Schema(description = "Data e hora da última alteração", example = "2026-02-20T14:35:00")
            LocalDateTime updatedAt) {
    }

    @Schema(description = "Requisição para criação de um token de senha")
    record SaveRequest(
            @Schema(description = "Token de redefinição de senha", example = "b7a1c3d4e5f6", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String token,
            @Schema(description = "Identificador interno do usuário", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idUser,
            @Schema(description = "Data e hora limite de validade do token", example = "2026-02-20T15:05:00", requiredMode = RequiredMode.REQUIRED)
            @NotNull LocalDateTime validUntil) {
    }

    @Schema(description = "Requisição para atualização de um token de senha")
    record UpdateRequest(
            @Schema(description = "Indica se o token já foi utilizado", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isUsed,
            @Schema(description = "Data e hora limite de validade do token", example = "2026-02-20T15:05:00")
            LocalDateTime validUntil) {
    }
}
