package dev.sentry.api.domain.organization.api;

import dev.sentry.api.domain.organization.Organization;
import org.mapstruct.Mapper;

/**
 * Só a direção de leitura. A escrita passa pelas fábricas e métodos da entidade — um
 * mapper capaz de escrever qualquer campo seria um furo no encapsulamento do agregado.
 */
@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    OrganizationRest.Response toDto(Organization organization);
}
