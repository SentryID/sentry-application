package dev.sentry.backend.roleoption;

import dev.sentry.api.domain.roleoption.api.RoleOptionRest;
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

@Tag(name = "Opções de perfil", description = "Operações de CRUD para as ações vinculadas a um perfil de acesso")
@RequestMapping("/role-options")
public interface RoleOptionControllerSwagger {

    @Operation(summary = "Lista opções de perfil", description = "Retorna as opções que atendem aos filtros informados, com paginação")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    List<RoleOptionRest.Response> findByQueryParams(@ParameterObject @Valid RoleOptionRest.QueryRequest queryParams);

    @Operation(summary = "Busca opção de perfil por ID", description = "Retorna os dados de uma opção específica")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opção encontrada"),
            @ApiResponse(responseCode = "404", description = "Opção de perfil não encontrada")
    })
    @GetMapping("/{id}")
    RoleOptionRest.Response findById(@PathVariable Long id);

    @Operation(summary = "Cria uma nova opção de perfil")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Opção criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Essa ação já está vinculada ao perfil")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void save(@RequestBody @Valid RoleOptionRest.SaveRequest request);

    @Operation(summary = "Atualiza uma opção de perfil existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Opção atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Opção de perfil não encontrada"),
            @ApiResponse(responseCode = "409", description = "Essa ação já está vinculada ao perfil")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable Long id, @RequestBody @Valid RoleOptionRest.UpdateRequest request);

    @Operation(summary = "Remove uma opção de perfil", description = "Exclusão lógica: marca a opção como excluída")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Opção removida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Opção de perfil não encontrada")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id);
}
