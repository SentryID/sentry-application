package dev.sentry.api.domain.system.api;

import dev.sentry.api.domain.system.System;
import org.mapstruct.Mapper;

/** Só a direção de leitura; a escrita passa pelas fábricas da entidade. */
@Mapper(componentModel = "spring")
public interface SystemMapper {

    SystemRest.Response toDto(System system);
}
