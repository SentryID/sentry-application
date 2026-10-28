package dev.sentry.api.domain.passwordtoken.api;

import dev.sentry.api.domain.passwordtoken.PasswordToken;
import dev.sentry.api.utils.ReferenceMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = ReferenceMapper.class)
public interface PasswordTokenMapper {

    @Mapping(target = "idUser", source = "user.id")
    PasswordTokenRest.Response toDto(PasswordToken passwordToken);

    @Mapping(target = "user", source = "idUser")
    PasswordToken toEntity(PasswordTokenRest.SaveRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(PasswordTokenRest.UpdateRequest dto, @MappingTarget PasswordToken entity);
}
