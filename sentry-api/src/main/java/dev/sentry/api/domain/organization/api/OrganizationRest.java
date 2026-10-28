package dev.sentry.api.domain.organization.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/** Contratos REST da organização: os records de request/response dos endpoints. */
public interface OrganizationRest {

    @Schema(description = "Requisição para consulta de organizações")
    record QueryRequest(
            @Schema(description = "Nome da organização (busca parcial, sem diferenciar maiúsculas)", example = "Sentry")
            String name,
            @Schema(description = "Código externo da organização", example = "ORG001")
            String externalCode,
            @Schema(description = "Identificador público da organização", example = "3f2504e0-4f89-11d3-9a0c-0305e82c3301")
            UUID uuid,
            @Schema(description = "Indica se a organização está ativa", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se a organização foi excluída logicamente", example = "false")
            Boolean isDeleted,
            @Schema(description = "Número da página para paginação", example = "0", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer page,
            @Schema(description = "Tamanho da página para paginação", example = "10", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer pageSize,
            @Schema(description = "Campo para ordenação dos resultados.", example = "id,asc;name,desc")
            String sortBy) {
    }

    @Schema(description = "Dados de uma organização")
    record Response(
            @Schema(description = "Identificador interno da organização", example = "1")
            Long id,
            @Schema(description = "Nome da organização", example = "Sentry ID")
            String name,
            @Schema(description = "Código externo da organização", example = "ORG001")
            String externalCode,
            @Schema(description = "Identificador público da organização", example = "3f2504e0-4f89-11d3-9a0c-0305e82c3301")
            UUID uuid,
            @Schema(description = "Indica se a organização está ativa", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se a organização foi excluída logicamente", example = "false")
            Boolean isDeleted,
            @Schema(description = "Usuário que criou o registro", example = "system")
            String createdBy,
            @Schema(description = "Data e hora de criação do registro", example = "2026-01-15T10:30:00")
            LocalDateTime createdAt,
            @Schema(description = "Usuário que realizou a última alteração", example = "system")
            String updatedBy,
            @Schema(description = "Data e hora da última alteração", example = "2026-02-20T14:05:00")
            LocalDateTime updatedAt) {
    }

    @Schema(description = "Requisição para criação de uma organização")
    record SaveRequest(
            @Schema(description = "Nome da organização", example = "Sentry ID", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String name,
            @Schema(description = "Código externo da organização", example = "ORG001")
            String externalCode,
            @Schema(description = "Indica se a organização está ativa", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }

    @Schema(description = "Requisição para atualização de uma organização")
    record UpdateRequest(
            @Schema(description = "Nome da organização", example = "Sentry ID", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String name,
            @Schema(description = "Código externo da organização", example = "ORG001")
            String externalCode,
            @Schema(description = "Indica se a organização está ativa", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }
}
