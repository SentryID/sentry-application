package dev.sentry.api.domain.access.action.api;

import dev.sentry.api.domain.access.action.Action;
import dev.sentry.api.utils.ReferenceMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = ReferenceMapper.class)
public interface ActionMapper {

    @Mapping(target = "idSystem", source = "system.id")
    ActionRest.Response toDto(Action action);

    @Mapping(target = "system", source = "idSystem")
    Action toEntity(ActionRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "system", source = "idSystem")
    void updateEntity(ActionRest.UpdateRequest dto, @MappingTarget Action entity);
}
