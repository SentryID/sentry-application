package dev.sentry.api.domain.roleuser.api;

import dev.sentry.api.domain.roleuser.RoleUser;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface RoleUserMapper {

    RoleUserRest.Response toDto(RoleUser roleUser);

    RoleUser toEntity(RoleUserRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(RoleUserRest.UpdateRequest dto, @MappingTarget RoleUser entity);
}
