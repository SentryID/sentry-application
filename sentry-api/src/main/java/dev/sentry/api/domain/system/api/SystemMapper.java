package dev.sentry.api.domain.system.api;

import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.utils.ReferenceMapper;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = ReferenceMapper.class)
public interface SystemMapper {

    @Mapping(target = "idOrganization", source = "organization.id")
    @Mapping(target = "redirectUris", expression = "java(uris(system, ExternalSystem.UriType.LOGIN))")
    @Mapping(target = "postLogoutRedirectUris", expression = "java(uris(system, ExternalSystem.UriType.LOGOUT))")
    SystemRest.Response toDto(ExternalSystem system);

    // URIs e segredo são tratados pelo SystemService, que valida e gera o hash
    @Mapping(target = "organization", source = "idOrganization")
    @Mapping(target = "redirectUris", ignore = true)
    @Mapping(target = "clientSecretHash", ignore = true)
    ExternalSystem toEntity(SystemRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "organization", source = "idOrganization")
    @Mapping(target = "redirectUris", ignore = true)
    @Mapping(target = "clientSecretHash", ignore = true)
    void updateEntity(SystemRest.UpdateRequest dto, @MappingTarget ExternalSystem entity);

    default List<String> uris(ExternalSystem system, ExternalSystem.UriType type) {
        return system.getRedirectUris().stream()
                .filter(redirectUri -> redirectUri.type() == type)
                .map(ExternalSystem.RedirectUri::uri)
                .sorted()
                .toList();
    }
}
