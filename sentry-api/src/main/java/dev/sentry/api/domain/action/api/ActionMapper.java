package dev.sentry.api.domain.action.api;

import dev.sentry.api.domain.action.Action;
import org.mapstruct.Mapper;

/** Só a direção de leitura; a escrita passa pelas fábricas da entidade. */
@Mapper(componentModel = "spring")
public interface ActionMapper {

    ActionRest.Response toDto(Action action);
}
