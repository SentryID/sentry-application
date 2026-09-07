package dev.sentry.api.domain.accesslog.api;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/** Contratos REST do log de acesso: os records de request/response dos endpoints. */
public interface AccessLogRest {

    @Schema(description = "Requisição para consulta de logs de acesso")
    record QueryRequest(
            @Schema(description = "Identificador interno do usuário", example = "1")
            Long idUser,
            @Schema(description = "Identificador interno do sistema", example = "1")
            Long idSystem,
            @Schema(description = "Resultado da tentativa de acesso", example = "SUCCESS")
            String attemptResult,
            @Schema(description = "Endereço IP ou host de origem (busca parcial)", example = "192.168.0.1")
            String ipHost,
            @Schema(description = "Data e hora inicial da tentativa", example = "2026-02-01T00:00:00")
            LocalDateTime attemptedAtFrom,
            @Schema(description = "Data e hora final da tentativa", example = "2026-02-28T23:59:59")
            LocalDateTime attemptedAtTo,
            @Schema(description = "Número da página para paginação", example = "0", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer page,
            @Schema(description = "Tamanho da página para paginação", example = "10", requiredMode = RequiredMode.REQUIRED)
            @NotNull Integer pageSize,
            @Schema(description = "Campo para ordenação dos resultados.", example = "attemptedAt,desc")
            String sortBy) {
    }

    @Schema(description = "Dados de um log de acesso")
    record Response(
            @Schema(description = "Identificador interno do log de acesso", example = "1")
            Long id,
            @Schema(description = "Identificador interno do usuário", example = "1")
            Long idUser,
            @Schema(description = "Data e hora da tentativa de acesso", example = "2026-02-20T14:05:00")
            LocalDateTime attemptedAt,
            @Schema(description = "Resultado da tentativa de acesso", example = "SUCCESS")
            String attemptResult,
            @Schema(description = "Endereço IP ou host de origem", example = "192.168.0.1")
            String ipHost,
            @Schema(description = "Identificador interno do sistema", example = "1")
            Long idSystem) {
    }

    @Schema(description = "Requisição para registro de um log de acesso")
    record SaveRequest(
            @Schema(description = "Identificador interno do usuário", example = "1")
            Long idUser,
            @Schema(description = "Resultado da tentativa de acesso", example = "SUCCESS", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String attemptResult,
            @Schema(description = "Endereço IP ou host de origem", example = "192.168.0.1")
            String ipHost,
            @Schema(description = "Identificador interno do sistema", example = "1")
            Long idSystem) {
    }

    @Schema(description = "Requisição para atualização de um log de acesso")
    record UpdateRequest(
            @Schema(description = "Identificador interno do usuário", example = "1")
            Long idUser,
            @Schema(description = "Resultado da tentativa de acesso", example = "SUCCESS", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String attemptResult,
            @Schema(description = "Endereço IP ou host de origem", example = "192.168.0.1")
            String ipHost,
            @Schema(description = "Identificador interno do sistema", example = "1")
            Long idSystem) {
    }
}
