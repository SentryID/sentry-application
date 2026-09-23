package dev.sentry.api.domain.access.roleuser.api;

import dev.sentry.api.domain.access.roleuser.RoleUser;
import dev.sentry.api.utils.ReferenceMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = ReferenceMapper.class)
public interface RoleUserMapper {

    @Mapping(target = "idRole", source = "role.id")
    @Mapping(target = "idUser", source = "user.id")
    RoleUserRest.Response toDto(RoleUser roleUser);

    @Mapping(target = "role", source = "idRole")
    @Mapping(target = "user", source = "idUser")
    RoleUser toEntity(RoleUserRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "role", source = "idRole")
    @Mapping(target = "user", source = "idUser")
    void updateEntity(RoleUserRest.UpdateRequest dto, @MappingTarget RoleUser entity);
}
