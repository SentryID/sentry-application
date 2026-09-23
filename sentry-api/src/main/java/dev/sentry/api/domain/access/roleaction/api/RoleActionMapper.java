package dev.sentry.api.domain.access.roleaction.api;

import dev.sentry.api.domain.access.roleaction.RoleAction;
import dev.sentry.api.utils.ReferenceMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = ReferenceMapper.class)
public interface RoleActionMapper {

    @Mapping(target = "idRole", source = "role.id")
    @Mapping(target = "cdAction", source = "action.id")
    RoleActionRest.Response toDto(RoleAction roleAction);

    @Mapping(target = "role", source = "idRole")
    @Mapping(target = "action", source = "cdAction")
    RoleAction toEntity(RoleActionRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "role", source = "idRole")
    @Mapping(target = "action", source = "cdAction")
    void updateEntity(RoleActionRest.UpdateRequest dto, @MappingTarget RoleAction entity);
}
