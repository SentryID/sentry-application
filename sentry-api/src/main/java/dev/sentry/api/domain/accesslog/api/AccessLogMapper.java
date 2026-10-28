package dev.sentry.api.domain.accesslog.api;

import dev.sentry.api.domain.accesslog.AccessLog;
import dev.sentry.api.utils.ReferenceMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = ReferenceMapper.class)
public interface AccessLogMapper {

    @Mapping(target = "idUser", source = "user.id")
    @Mapping(target = "idSystem", source = "system.id")
    AccessLogRest.Response toDto(AccessLog accessLog);

    @Mapping(target = "user", source = "idUser")
    @Mapping(target = "system", source = "idSystem")
    AccessLog toEntity(AccessLogRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "user", source = "idUser")
    @Mapping(target = "system", source = "idSystem")
    void updateEntity(AccessLogRest.UpdateRequest dto, @MappingTarget AccessLog entity);
}
