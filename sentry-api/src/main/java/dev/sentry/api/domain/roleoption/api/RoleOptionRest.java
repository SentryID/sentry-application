package dev.sentry.api.domain.roleoption.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Contratos REST da opção de perfil: os records de request/response dos endpoints. */
public interface RoleOptionRest {

    @Schema(description = "Requisição para consulta de opções de perfil")
    record QueryRequest(
            @Schema(description = "Identificador interno do perfil de acesso", example = "1")
            Long idRole,
            @Schema(description = "Código da ação vinculada", example = "1")
            Long cdAction,
            @Schema(description = "Indica se a opção está ativa", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se a opção foi excluída logicamente", example = "false")
            Boolean isDeleted,
            @Schema(description = "Número da página para paginação", example = "0", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer page,
            @Schema(description = "Tamanho da página para paginação", example = "10", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer pageSize,
            @Schema(description = "Campo para ordenação dos resultados.", example = "id,asc;idRole,desc")
            String sortBy) {
    }

    @Schema(description = "Dados de uma opção de perfil")
    record Response(
            @Schema(description = "Identificador interno da opção de perfil", example = "1")
            Long id,
            @Schema(description = "Identificador interno do perfil de acesso", example = "1")
            Long idRole,
            @Schema(description = "Código da ação vinculada", example = "1")
            Long cdAction,
            @Schema(description = "Indica se a opção está ativa", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se a opção foi excluída logicamente", example = "false")
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

    @Schema(description = "Requisição para criação de uma opção de perfil")
    record SaveRequest(
            @Schema(description = "Identificador interno do perfil de acesso", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idRole,
            @Schema(description = "Código da ação vinculada", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long cdAction,
            @Schema(description = "Indica se a opção está ativa", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }

    @Schema(description = "Requisição para atualização de uma opção de perfil")
    record UpdateRequest(
            @Schema(description = "Identificador interno do perfil de acesso", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idRole,
            @Schema(description = "Código da ação vinculada", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long cdAction,
            @Schema(description = "Indica se a opção está ativa", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }
}
