package dev.sentry.api.domain.mfatoken.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Contratos REST do token de MFA: os records de request/response dos endpoints. */
public interface MfaTokenRest {

    @Schema(description = "Requisição para consulta de tokens de MFA")
    record QueryRequest(
            @Schema(description = "Código enviado ao usuário", example = "123456")
            String code,
            @Schema(description = "Token de identificação do desafio", example = "b7a1c3d4e5f6")
            String token,
            @Schema(description = "Login do usuário (busca parcial, sem diferenciar maiúsculas)", example = "maria.silva")
            String username,
            @Schema(description = "Indica se o token já foi utilizado", example = "false")
            Boolean isUsed,
            @Schema(description = "Número da página para paginação", example = "0", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer page,
            @Schema(description = "Tamanho da página para paginação", example = "10", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer pageSize,
            @Schema(description = "Campo para ordenação dos resultados.", example = "id,asc;createdAt,desc")
            String sortBy) {
    }

    @Schema(description = "Dados de um token de MFA")
    record Response(
            @Schema(description = "Identificador interno do token de MFA", example = "1")
            Long id,
            @Schema(description = "Código enviado ao usuário", example = "123456")
            String code,
            @Schema(description = "Token de identificação do desafio", example = "b7a1c3d4e5f6")
            String token,
            @Schema(description = "Login do usuário", example = "maria.silva")
            String username,
            @Schema(description = "Indica se o token já foi utilizado", example = "false")
            Boolean isUsed,
            @Schema(description = "Data e hora limite de validade do token", example = "2026-02-20T14:10:00")
            LocalDateTime validUntil,
            @Schema(description = "Data e hora de criação do registro", example = "2026-02-20T14:05:00")
            LocalDateTime createdAt,
            @Schema(description = "Data e hora em que o token foi utilizado", example = "2026-02-20T14:07:00")
            LocalDateTime usedAt) {
    }

    @Schema(description = "Requisição para criação de um token de MFA")
    record SaveRequest(
            @Schema(description = "Código enviado ao usuário", example = "123456", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String code,
            @Schema(description = "Token de identificação do desafio", example = "b7a1c3d4e5f6", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String token,
            @Schema(description = "Login do usuário", example = "maria.silva", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String username,
            @Schema(description = "Data e hora limite de validade do token", example = "2026-02-20T14:10:00", requiredMode = RequiredMode.REQUIRED)
            @NotNull LocalDateTime validUntil) {
    }

    @Schema(description = "Requisição para atualização de um token de MFA")
    record UpdateRequest(
            @Schema(description = "Indica se o token já foi utilizado", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isUsed,
            @Schema(description = "Data e hora limite de validade do token", example = "2026-02-20T14:10:00")
            LocalDateTime validUntil,
            @Schema(description = "Data e hora em que o token foi utilizado", example = "2026-02-20T14:07:00")
            LocalDateTime usedAt) {
    }
}
