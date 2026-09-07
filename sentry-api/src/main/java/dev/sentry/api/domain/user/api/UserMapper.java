package dev.sentry.api.domain.user.api;

import dev.sentry.api.domain.user.Email;
import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.Username;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Só a direção de leitura, e sem hash nem salt no {@code Response} — a escrita passa
 * pelas fábricas de {@link User}.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "authenticationType", source = "credentials.authenticationType")
    UserRest.Response toDto(User user);

    default String map(Email email) {
        return email == null ? null : email.value();
    }

    default String map(Username username) {
        return username == null ? null : username.value();
    }
}
