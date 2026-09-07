package dev.sentry.api.domain.user.api;

import dev.sentry.api.domain.user.Email;
import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.Username;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends ListCrudRepository<User, Long>, JpaSpecificationExecutor<User> {

    User findByUsername(Username username);

    User findByEmail(Email email);
}
