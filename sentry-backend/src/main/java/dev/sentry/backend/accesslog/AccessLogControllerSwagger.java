package dev.sentry.backend.accesslog;

import dev.sentry.api.domain.accesslog.api.AccessLogRest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Logs de acesso", description = "Operações de CRUD para os logs de tentativa de acesso")
@RequestMapping("/access-logs")
public interface AccessLogControllerSwagger {

    @Operation(summary = "Lista logs de acesso", description = "Retorna os logs que atendem aos filtros informados, com paginação")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    List<AccessLogRest.Response> findByQueryParams(@ParameterObject @Valid AccessLogRest.QueryRequest queryParams);

    @Operation(summary = "Busca log de acesso por ID", description = "Retorna os dados de um log específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Log encontrado"),
            @ApiResponse(responseCode = "404", description = "Log de acesso não encontrado")
    })
    @GetMapping("/{id}")
    AccessLogRest.Response findById(@PathVariable Long id);

    @Operation(summary = "Registra um novo log de acesso", description = "A data e hora da tentativa é preenchida automaticamente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Log registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void save(@RequestBody @Valid AccessLogRest.SaveRequest request);

    @Operation(summary = "Atualiza um log de acesso existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Log atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Log de acesso não encontrado")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable Long id, @RequestBody @Valid AccessLogRest.UpdateRequest request);

    @Operation(summary = "Remove um log de acesso", description = "A tabela não tem exclusão lógica: o log é removido de fato")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Log removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Log de acesso não encontrado")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id);
}
