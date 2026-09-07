package dev.sentry.api.domain.role.api;

import dev.sentry.api.domain.role.Role;
import dev.sentry.api.domain.role.RoleOption;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * Só a direção de leitura. O {@code Response} do perfil não traz as opções — elas vêm
 * pelo sub-recurso {@code /roles/{id}/options}, o que evita N+1 na listagem.
 */
@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleRest.Response toDto(Role role);

    RoleRest.OptionResponse toOptionDto(RoleOption option);

    List<RoleRest.OptionResponse> toOptionDtos(List<RoleOption> options);
}
