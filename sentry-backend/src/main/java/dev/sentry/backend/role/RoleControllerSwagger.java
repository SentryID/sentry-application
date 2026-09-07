package dev.sentry.backend.role;

import dev.sentry.api.domain.role.api.RoleRest;
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

@Tag(name = "Perfis de acesso", description = "Operações de CRUD para perfis de acesso")
@RequestMapping("/roles")
public interface RoleControllerSwagger {

    @Operation(summary = "Lista perfis de acesso", description = "Retorna os perfis que atendem aos filtros informados, com paginação")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    List<RoleRest.Response> findByQueryParams(@ParameterObject @Valid RoleRest.QueryRequest queryParams);

    @Operation(summary = "Busca perfil de acesso por ID", description = "Retorna os dados de um perfil específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "404", description = "Perfil de acesso não encontrado")
    })
    @GetMapping("/{id}")
    RoleRest.Response findById(@PathVariable Long id);

    @Operation(summary = "Cria um novo perfil de acesso")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Perfil criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Já existe um perfil com esse nome nesse sistema")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void save(@RequestBody @Valid RoleRest.SaveRequest request);

    @Operation(summary = "Atualiza um perfil de acesso existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Perfil de acesso não encontrado"),
            @ApiResponse(responseCode = "409", description = "Já existe um perfil com esse nome nesse sistema")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable Long id, @RequestBody @Valid RoleRest.UpdateRequest request);

    @Operation(summary = "Remove um perfil de acesso", description = "Exclusão lógica: marca o perfil como excluído")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Perfil removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Perfil de acesso não encontrado")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id);
}
