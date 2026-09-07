package dev.sentry.api.domain.role.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Contratos REST do perfil de acesso: os records de request/response dos endpoints. */
public interface RoleRest {

    @Schema(description = "Requisição para consulta de perfis de acesso")
    record QueryRequest(
            @Schema(description = "Identificador interno do sistema", example = "1")
            Long idSystem,
            @Schema(description = "Identificador interno da organização", example = "1")
            Long idOrganization,
            @Schema(description = "Perfil interno do sistema (busca parcial, sem diferenciar maiúsculas)", example = "ADMIN")
            String systemRole,
            @Schema(description = "Perfil externo correspondente (busca parcial, sem diferenciar maiúsculas)", example = "ADMINISTRADOR")
            String externalRole,
            @Schema(description = "Tipo do perfil de acesso", example = "INTERNAL")
            String roleType,
            @Schema(description = "Indica se o perfil está ativo", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se o perfil foi excluído logicamente", example = "false")
            Boolean isDeleted,
            @Schema(description = "Número da página para paginação", example = "0", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer page,
            @Schema(description = "Tamanho da página para paginação", example = "10", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer pageSize,
            @Schema(description = "Campo para ordenação dos resultados.", example = "id,asc;systemRole,desc")
            String sortBy) {
    }

    @Schema(description = "Dados de um perfil de acesso")
    record Response(
            @Schema(description = "Identificador interno do perfil de acesso", example = "1")
            Long id,
            @Schema(description = "Identificador interno do sistema", example = "1")
            Long idSystem,
            @Schema(description = "Identificador interno da organização", example = "1")
            Long idOrganization,
            @Schema(description = "Perfil interno do sistema", example = "ADMIN")
            String systemRole,
            @Schema(description = "Perfil externo correspondente", example = "ADMINISTRADOR")
            String externalRole,
            @Schema(description = "Tipo do perfil de acesso", example = "INTERNAL")
            String roleType,
            @Schema(description = "Indica se o perfil está ativo", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se o perfil foi excluído logicamente", example = "false")
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

    @Schema(description = "Requisição para criação de um perfil de acesso")
    record SaveRequest(
            @Schema(description = "Identificador interno do sistema", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idSystem,
            @Schema(description = "Identificador interno da organização", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idOrganization,
            @Schema(description = "Perfil interno do sistema", example = "ADMIN", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String systemRole,
            @Schema(description = "Perfil externo correspondente", example = "ADMINISTRADOR")
            String externalRole,
            @Schema(description = "Tipo do perfil de acesso", example = "INTERNAL")
            String roleType,
            @Schema(description = "Indica se o perfil está ativo", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }

    @Schema(description = "Ação concedida a um perfil de acesso")
    record OptionResponse(
            @Schema(description = "Identificador interno da opção de perfil", example = "1")
            Long id,
            @Schema(description = "Código da ação concedida", example = "1")
            Long cdAction,
            @Schema(description = "Indica se a opção está ativa", example = "true")
            Boolean isActive,
            @Schema(description = "Usuário que criou o registro", example = "system")
            String createdBy,
            @Schema(description = "Data e hora de criação do registro", example = "2026-01-15T10:30:00")
            LocalDateTime createdAt) {
    }

    @Schema(description = "Requisição para conceder uma ação a um perfil de acesso")
    record GrantRequest(
            @Schema(description = "Código da ação a conceder", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long cdAction) {
    }

    @Schema(description = "Requisição para atualização de um perfil de acesso")
    record UpdateRequest(
            @Schema(description = "Identificador interno do sistema", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idSystem,
            @Schema(description = "Identificador interno da organização", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idOrganization,
            @Schema(description = "Perfil interno do sistema", example = "ADMIN", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String systemRole,
            @Schema(description = "Perfil externo correspondente", example = "ADMINISTRADOR")
            String externalRole,
            @Schema(description = "Tipo do perfil de acesso", example = "INTERNAL")
            String roleType,
            @Schema(description = "Indica se o perfil está ativo", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }
}
