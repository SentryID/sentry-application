package dev.sentry.api.domain.system.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

/** Contratos REST do sistema: os records de request/response dos endpoints. */
public interface SystemRest {

    @Schema(description = "Requisição para consulta de sistemas")
    record QueryRequest(
            @Schema(description = "Identificador interno da organização dona do sistema", example = "1")
            Long idOrganization,
            @Schema(description = "Código externo do sistema", example = "SYS001")
            String externalCode,
            @Schema(description = "Descrição do sistema (busca parcial, sem diferenciar maiúsculas)", example = "Portal")
            String description,
            @Schema(description = "URL do sistema (busca parcial, sem diferenciar maiúsculas)", example = "https://portal.sentry.dev")
            String url,
            @Schema(description = "Identificador público do sistema", example = "3f2504e0-4f89-11d3-9a0c-0305e82c3301")
            UUID uuid,
            @Schema(description = "Indica se o sistema está ativo", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se o sistema foi excluído logicamente", example = "false")
            Boolean isDeleted,
            @Schema(description = "Número da página para paginação", example = "0", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer page,
            @Schema(description = "Tamanho da página para paginação", example = "10", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer pageSize,
            @Schema(description = "Campo para ordenação dos resultados.", example = "id,asc;description,desc")
            String sortBy) {
    }

    @Schema(description = "Dados de um sistema")
    record Response(
            @Schema(description = "Identificador interno do sistema", example = "1")
            Long id,
            @Schema(description = "Identificador interno da organização dona do sistema", example = "1")
            Long idOrganization,
            @Schema(description = "Código externo do sistema", example = "SYS001")
            String externalCode,
            @Schema(description = "Descrição do sistema", example = "Portal do Cliente")
            String description,
            @Schema(description = "URL do sistema", example = "https://portal.sentry.dev")
            String url,
            @Schema(description = "Escopos suportados pelo sistema", example = "read write")
            String scopes,
            @Schema(description = "Identificador público do sistema", example = "3f2504e0-4f89-11d3-9a0c-0305e82c3301")
            UUID uuid,
            @Schema(description = "Indica se o sistema está ativo", example = "true")
            Boolean isActive,
            @Schema(description = "Indica se o sistema foi excluído logicamente", example = "false")
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

    @Schema(description = "Requisição para criação de um sistema")
    record SaveRequest(
            @Schema(description = "Identificador interno da organização dona do sistema", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idOrganization,
            @Schema(description = "Código externo do sistema", example = "SYS001")
            String externalCode,
            @Schema(description = "Descrição do sistema", example = "Portal do Cliente", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String description,
            @Schema(description = "URL do sistema", example = "https://portal.sentry.dev")
            String url,
            @Schema(description = "Escopos suportados pelo sistema", example = "read write")
            String scopes,
            @Schema(description = "Indica se o sistema está ativo", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }

    @Schema(description = "Requisição para atualização de um sistema")
    record UpdateRequest(
            @Schema(description = "Identificador interno da organização dona do sistema", example = "1", requiredMode = RequiredMode.REQUIRED)
            @NotNull Long idOrganization,
            @Schema(description = "Código externo do sistema", example = "SYS001")
            String externalCode,
            @Schema(description = "Descrição do sistema", example = "Portal do Cliente", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String description,
            @Schema(description = "URL do sistema", example = "https://portal.sentry.dev")
            String url,
            @Schema(description = "Escopos suportados pelo sistema", example = "read write")
            String scopes,
            @Schema(description = "Indica se o sistema está ativo", example = "true", requiredMode = RequiredMode.REQUIRED)
            @NotNull Boolean isActive) {
    }
}
