package dev.sentry.api.domain.roleoption.api;

import dev.sentry.api.domain.roleoption.RoleOption;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface RoleOptionMapper {

    RoleOptionRest.Response toDto(RoleOption roleOption);

    RoleOption toEntity(RoleOptionRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(RoleOptionRest.UpdateRequest dto, @MappingTarget RoleOption entity);
}
