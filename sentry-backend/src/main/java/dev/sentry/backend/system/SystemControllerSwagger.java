package dev.sentry.backend.system;

import dev.sentry.api.domain.system.api.SystemRest;
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

@Tag(name = "Sistemas", description = "Operações de CRUD para sistemas")
@RequestMapping("/systems")
public interface SystemControllerSwagger {

    @Operation(summary = "Lista sistemas", description = "Retorna os sistemas que atendem aos filtros informados, com paginação")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    List<SystemRest.Response> findByQueryParams(@ParameterObject @Valid SystemRest.QueryRequest queryParams);

    @Operation(summary = "Busca sistema por ID", description = "Retorna os dados de um sistema específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sistema encontrado"),
            @ApiResponse(responseCode = "404", description = "Sistema não encontrado")
    })
    @GetMapping("/{id}")
    SystemRest.Response findById(@PathVariable Long id);

    @Operation(summary = "Cria um novo sistema")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sistema criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Já existe um sistema com esse código externo")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void save(@RequestBody @Valid SystemRest.SaveRequest request);

    @Operation(summary = "Atualiza um sistema existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sistema atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Sistema não encontrado"),
            @ApiResponse(responseCode = "409", description = "Já existe um sistema com esse código externo")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable Long id, @RequestBody @Valid SystemRest.UpdateRequest request);

    @Operation(summary = "Remove um sistema", description = "Exclusão lógica: marca o sistema como excluído")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sistema removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Sistema não encontrado")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id);
}
