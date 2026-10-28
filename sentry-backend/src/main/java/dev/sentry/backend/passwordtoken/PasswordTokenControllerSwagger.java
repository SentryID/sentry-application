package dev.sentry.backend.passwordtoken;

import dev.sentry.api.domain.passwordtoken.api.PasswordTokenRest;
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

@Tag(name = "Tokens de senha", description = "Operações de CRUD para tokens de redefinição de senha")
@RequestMapping("/password-tokens")
public interface PasswordTokenControllerSwagger {

    @Operation(summary = "Lista tokens de senha", description = "Retorna os tokens que atendem aos filtros informados, com paginação")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    @GetMapping
    List<PasswordTokenRest.Response> findByQueryParams(
            @ParameterObject @Valid PasswordTokenRest.QueryRequest queryParams);

    @Operation(summary = "Busca token de senha por ID", description = "Retorna os dados de um token específico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token encontrado"),
            @ApiResponse(responseCode = "404", description = "Token de senha não encontrado")
    })
    @GetMapping("/{id}")
    PasswordTokenRest.Response findById(@PathVariable Long id);

    @Operation(summary = "Cria um novo token de senha")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Token criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Já existe um token de senha com esse valor")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void save(@RequestBody @Valid PasswordTokenRest.SaveRequest request);

    @Operation(summary = "Atualiza um token de senha existente")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token atualizado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Token de senha não encontrado")
    })
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void update(@PathVariable Long id, @RequestBody @Valid PasswordTokenRest.UpdateRequest request);

    @Operation(summary = "Remove um token de senha", description = "A tabela não tem exclusão lógica: o token é removido de fato")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Token de senha não encontrado")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id);
}
