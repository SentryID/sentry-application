package dev.sentry.api.domain.system.api;

import dev.sentry.api.domain.system.System;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SystemMapper {

    SystemRest.Response toDto(System system);

    System toEntity(SystemRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(SystemRest.UpdateRequest dto, @MappingTarget System entity);
}
