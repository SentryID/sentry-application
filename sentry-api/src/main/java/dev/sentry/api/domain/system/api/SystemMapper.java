package dev.sentry.api.domain.system.api;

import dev.sentry.api.domain.system.System;
import dev.sentry.api.utils.ReferenceMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = ReferenceMapper.class)
public interface SystemMapper {

    @Mapping(target = "idOrganization", source = "organization.id")
    SystemRest.Response toDto(System system);

    @Mapping(target = "organization", source = "idOrganization")
    System toEntity(SystemRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "organization", source = "idOrganization")
    void updateEntity(SystemRest.UpdateRequest dto, @MappingTarget System entity);
}
