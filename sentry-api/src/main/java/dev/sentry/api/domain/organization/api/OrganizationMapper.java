package dev.sentry.api.domain.organization.api;

import dev.sentry.api.domain.organization.Organization;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    OrganizationRest.Response toDto(Organization organization);

    Organization toEntity(OrganizationRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(OrganizationRest.UpdateRequest dto, @MappingTarget Organization entity);
}
