package dev.sentry.api.domain.mfatoken.api;

import dev.sentry.api.domain.mfatoken.MfaToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Só a direção de leitura; a emissão passa por {@code MfaToken.issue}. */
@Mapper(componentModel = "spring")
public interface MfaTokenMapper {

    @Mapping(target = "isUsed", source = "validity.isUsed")
    @Mapping(target = "validUntil", source = "validity.validUntil")
    @Mapping(target = "usedAt", source = "validity.usedAt")
    MfaTokenRest.Response toDto(MfaToken mfaToken);
}
