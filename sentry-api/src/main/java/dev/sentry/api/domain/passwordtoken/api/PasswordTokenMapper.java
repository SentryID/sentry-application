package dev.sentry.api.domain.passwordtoken.api;

import dev.sentry.api.domain.passwordtoken.PasswordToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Só a direção de leitura; a emissão passa por {@code PasswordToken.issue}. */
@Mapper(componentModel = "spring")
public interface PasswordTokenMapper {

    @Mapping(target = "isUsed", source = "validity.isUsed")
    @Mapping(target = "validUntil", source = "validity.validUntil")
    PasswordTokenRest.Response toDto(PasswordToken passwordToken);
}
