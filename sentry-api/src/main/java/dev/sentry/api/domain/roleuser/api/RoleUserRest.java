package dev.sentry.api.domain.roleuser.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Contratos REST do vínculo entre perfil e usuário: os records de request/response dos endpoints. */
public interface RoleUserRest {

    @Schema(description = "Requisição para consulta de vínculos entre perfil e usuário")
    record QueryRequest(
            @Schema(description = "Identificador interno do grupo de perfis", example = "1")
            Long idGroup,
            @Schema(description = "Identificador interno do usuário", example = "1")
            Long idUser,
            @Schema(description = "Indica se o vínculo está ativo", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se o vínculo foi excluído logicamente", example = "false")
            Boolean isDeleted,
            @Schema(description = "Número da página para paginação", example = "0", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer page,
            @Schema(description = "Tamanho da página para paginação", example = "10", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer pageSize,
            @Schema(description = "Campo para ordenação dos resultados.", example = "id,asc;idUser,desc")
            String sortBy) {
    }

    @Schema(description = "Dados de um vínculo entre perfil e usuário")
    record Response(
            @Schema(description = "Identificador interno do vínculo", example = "1")
            Long id,
            @Schema(description = "Identificador interno do grupo de perfis", example = "1")
            Long idGroup,
            @Schema(description = "Identificador interno do usuário", example = "1")
            Long idUser,
            @Schema(description = "Indica se o vínculo está ativo", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se o vínculo foi excluído logicamente", example = "false")
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

    @Schema(description = "Requisição para criação de um vínculo entre perfil e usuário")
    record SaveRequest(
            @Schema(description = "Identificador interno do grupo de perfis", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idGroup,
            @Schema(description = "Identificador interno do usuário", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idUser,
            @Schema(description = "Indica se o vínculo está ativo", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }

    @Schema(description = "Requisição para atualização de um vínculo entre perfil e usuário")
    record UpdateRequest(
            @Schema(description = "Identificador interno do grupo de perfis", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idGroup,
            @Schema(description = "Identificador interno do usuário", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idUser,
            @Schema(description = "Indica se o vínculo está ativo", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }
}
