package dev.sentry.api.domain.role.api;

import dev.sentry.api.domain.role.Role;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleRest.Response toDto(Role role);

    Role toEntity(RoleRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(RoleRest.UpdateRequest dto, @MappingTarget Role entity);
}
