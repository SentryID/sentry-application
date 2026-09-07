package dev.sentry.backend.roleuser;

import dev.sentry.api.domain.roleuser.api.RoleUserRest;
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

@Tag(name = "Perfis dos usuários", description = "Operações de CRUD para o vínculo entre perfis e usuários")
@RequestMapping("/role-users")
public interface RoleUserControllerSwagger {

    @Operation(summary = "Lista vínculos", description = "Retorna os vínculos que atendem aos filtros informados, com paginação")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    List<RoleUserRest.Response> findByQueryParams(@ParameterObject @Valid RoleUserRest.QueryRequest queryParams);

    @Operation(summary = "Busca vínculo por ID", description = "Retorna os dados de um vínculo específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vínculo encontrado"),
            @ApiResponse(responseCode = "404", description = "Vínculo não encontrado")
    })
    @GetMapping("/{id}")
    RoleUserRest.Response findById(@PathVariable Long id);

    @Operation(summary = "Cria um novo vínculo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vínculo criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Esse usuário já está vinculado a esse grupo")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void save(@RequestBody @Valid RoleUserRest.SaveRequest request);

    @Operation(summary = "Atualiza um vínculo existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vínculo atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Vínculo não encontrado"),
            @ApiResponse(responseCode = "409", description = "Esse usuário já está vinculado a esse grupo")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable Long id, @RequestBody @Valid RoleUserRest.UpdateRequest request);

    @Operation(summary = "Remove um vínculo", description = "Exclusão lógica: marca o vínculo como excluído")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vínculo removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Vínculo não encontrado")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id);
}
