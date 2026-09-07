package dev.sentry.api.domain.passwordtoken.api;

import dev.sentry.api.domain.passwordtoken.PasswordToken;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface PasswordTokenMapper {

    PasswordTokenRest.Response toDto(PasswordToken passwordToken);

    PasswordToken toEntity(PasswordTokenRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(PasswordTokenRest.UpdateRequest dto, @MappingTarget PasswordToken entity);
}
