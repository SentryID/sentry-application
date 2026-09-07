package dev.sentry.api.domain.accesslog.api;

import dev.sentry.api.domain.accesslog.AccessLog;
import org.mapstruct.Mapper;

/** Só a direção de leitura; o registro nasce por {@code AccessLog.record}. */
@Mapper(componentModel = "spring")
public interface AccessLogMapper {

    AccessLogRest.Response toDto(AccessLog accessLog);
}
