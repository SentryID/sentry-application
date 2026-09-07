package dev.sentry.api.domain.action.api;

import dev.sentry.api.domain.action.Action;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ActionMapper {

    ActionRest.Response toDto(Action action);

    Action toEntity(ActionRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(ActionRest.UpdateRequest dto, @MappingTarget Action entity);
}
