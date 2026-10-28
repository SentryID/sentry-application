package dev.sentry.backend.organization;

import dev.sentry.api.domain.organization.api.OrganizationRest;
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

@Tag(name = "Organizações", description = "Operações de CRUD para organizações")
@RequestMapping("/organizations")
public interface OrganizationControllerSwagger {

    @Operation(summary = "Lista organizações", description = "Retorna as organizações que atendem aos filtros informados, com paginação")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    List<OrganizationRest.Response> findByQueryParams(@ParameterObject @Valid OrganizationRest.QueryRequest queryParams);

    @Operation(summary = "Busca organização por ID", description = "Retorna os dados de uma organização específica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Organização encontrada"),
            @ApiResponse(responseCode = "404", description = "Organização não encontrada")
    })
    @GetMapping("/{id}")
    OrganizationRest.Response findById(@PathVariable Long id);

    @Operation(summary = "Cria uma nova organização")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Organização criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Já existe uma organização com esse código externo")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void save(@RequestBody @Valid OrganizationRest.SaveRequest request);

    @Operation(summary = "Atualiza uma organização existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Organização atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Organização não encontrada")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable Long id, @RequestBody @Valid OrganizationRest.UpdateRequest request);

    @Operation(summary = "Remove uma organização", description = "Exclusão lógica: marca a organização como excluída")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Organização removida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Organização não encontrada")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id);
}
