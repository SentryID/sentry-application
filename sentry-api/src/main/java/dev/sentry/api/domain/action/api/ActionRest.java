package dev.sentry.api.domain.action.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Contratos REST da ação: os records de request/response dos endpoints. */
public interface ActionRest {

    @Schema(description = "Requisição para consulta de ações")
    record QueryRequest(
            @Schema(description = "Tipo da ação (busca parcial, sem diferenciar maiúsculas)", example = "READ")
            String actionType,
            @Schema(description = "Identificador interno do sistema", example = "1")
            Long idSystem,
            @Schema(description = "Indica se a ação está ativa", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se a ação foi excluída logicamente", example = "false")
            Boolean isDeleted,
            @Schema(description = "Número da página para paginação", example = "0", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer page,
            @Schema(description = "Tamanho da página para paginação", example = "10", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer pageSize,
            @Schema(description = "Campo para ordenação dos resultados.", example = "id,asc;actionType,desc")
            String sortBy) {
    }

    @Schema(description = "Dados de uma ação")
    record Response(
            @Schema(description = "Código da ação", example = "1")
            Long id,
            @Schema(description = "Tipo da ação", example = "READ")
            String actionType,
            @Schema(description = "Identificador interno do sistema", example = "1")
            Long idSystem,
            @Schema(description = "Indica se a ação está ativa", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se a ação foi excluída logicamente", example = "false")
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

    @Schema(description = "Requisição para criação de uma ação")
    record SaveRequest(
            @Schema(description = "Tipo da ação", example = "READ", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String actionType,
            @Schema(description = "Identificador interno do sistema", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idSystem,
            @Schema(description = "Indica se a ação está ativa", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }

    @Schema(description = "Requisição para atualização de uma ação")
    record UpdateRequest(
            @Schema(description = "Tipo da ação", example = "READ", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String actionType,
            @Schema(description = "Identificador interno do sistema", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idSystem,
            @Schema(description = "Indica se a ação está ativa", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }
}
