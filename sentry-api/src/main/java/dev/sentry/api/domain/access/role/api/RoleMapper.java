package dev.sentry.api.domain.access.role.api;

import dev.sentry.api.domain.access.role.Role;
import dev.sentry.api.utils.ReferenceMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = ReferenceMapper.class)
public interface RoleMapper {

    @Mapping(target = "idSystem", source = "system.id")
    @Mapping(target = "idOrganization", source = "organization.id")
    RoleRest.Response toDto(Role role);

    @Mapping(target = "system", source = "idSystem")
    @Mapping(target = "organization", source = "idOrganization")
    Role toEntity(RoleRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "system", source = "idSystem")
    @Mapping(target = "organization", source = "idOrganization")
    void updateEntity(RoleRest.UpdateRequest dto, @MappingTarget Role entity);
}
