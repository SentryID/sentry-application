package dev.sentry.api.domain.roleuser.api;

import dev.sentry.api.domain.roleuser.RoleUser;
import org.mapstruct.Mapper;

/** Só a direção de leitura; a escrita passa pelas fábricas da entidade. */
@Mapper(componentModel = "spring")
public interface RoleUserMapper {

    RoleUserRest.Response toDto(RoleUser roleUser);
}
