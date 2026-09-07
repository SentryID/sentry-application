package dev.sentry.backend.role;

import dev.sentry.api.domain.role.api.RoleRest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Sub-recurso do perfil: as opções pertencem ao agregado {@code Role} e por isso não têm
 * rota própria de primeiro nível.
 */
@Tag(name = "Opções de perfil", description = "Ações concedidas a um perfil de acesso")
@RequestMapping("/roles/{idRole}/options")
public interface RoleOptionControllerSwagger {

    @Operation(summary = "Lista as ações concedidas ao perfil", description = "Traz apenas as opções vigentes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Perfil de acesso não encontrado")
    })
    @GetMapping
    List<RoleRest.OptionResponse> findOptions(@PathVariable Long idRole);

    @Operation(summary = "Concede uma ação ao perfil",
            description = "A ação precisa pertencer ao mesmo sistema do perfil. Conceder de novo o que já está concedido não faz nada")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ação concedida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Perfil de acesso ou ação não encontrados"),
            @ApiResponse(responseCode = "409", description = "A ação pertence a outro sistema")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    void grantOption(@PathVariable Long idRole, @RequestBody @Valid RoleRest.GrantRequest request);

    @Operation(summary = "Revoga uma ação do perfil", description = "Exclusão lógica da opção")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ação revogada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Perfil de acesso não encontrado, ou ação não vinculada a ele")
    })
    @DeleteMapping("/{cdAction}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revokeOption(@PathVariable Long idRole, @PathVariable Long cdAction);
}
