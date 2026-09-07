package dev.sentry.backend.action;

import dev.sentry.api.domain.action.api.ActionRest;
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

@Tag(name = "Ações", description = "Operações de CRUD para ações")
@RequestMapping("/actions")
public interface ActionControllerSwagger {

    @Operation(summary = "Lista ações", description = "Retorna as ações que atendem aos filtros informados, com paginação")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    List<ActionRest.Response> findByQueryParams(@ParameterObject @Valid ActionRest.QueryRequest queryParams);

    @Operation(summary = "Busca ação por código", description = "Retorna os dados de uma ação específica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ação encontrada"),
            @ApiResponse(responseCode = "404", description = "Ação não encontrada")
    })
    @GetMapping("/{id}")
    ActionRest.Response findById(@PathVariable Long id);

    @Operation(summary = "Cria uma nova ação")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ação criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Já existe uma ação desse tipo nesse sistema")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void save(@RequestBody @Valid ActionRest.SaveRequest request);

    @Operation(summary = "Atualiza uma ação existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ação atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ação não encontrada"),
            @ApiResponse(responseCode = "409", description = "Já existe uma ação desse tipo nesse sistema")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable Long id, @RequestBody @Valid ActionRest.UpdateRequest request);

    @Operation(summary = "Remove uma ação", description = "Exclusão lógica: marca a ação como excluída")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ação removida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Ação não encontrada")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id);
}
