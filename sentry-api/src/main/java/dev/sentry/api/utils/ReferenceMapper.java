package dev.sentry.api.utils;

import dev.sentry.api.domain.access.action.Action;
import dev.sentry.api.domain.access.role.Role;
import dev.sentry.api.domain.organization.Organization;
import dev.sentry.api.domain.system.System;
import dev.sentry.api.domain.user.User;
import org.springframework.stereotype.Component;

/** Converte identificadores vindos dos DTOs em referências de entidade para os relacionamentos JPA. */
@Component
public class ReferenceMapper {

    public System toSystem(Long id) {
        if (id == null) {
            return null;
        }
        System system = new System();
        system.setId(id);
        return system;
    }

    public Organization toOrganization(Long id) {
        if (id == null) {
            return null;
        }
        Organization organization = new Organization();
        organization.setId(id);
        return organization;
    }

    public User toUser(Long id) {
        if (id == null) {
            return null;
        }
        User user = new User();
        user.setId(id);
        return user;
    }

    public Role toRole(Long id) {
        if (id == null) {
            return null;
        }
        Role role = new Role();
        role.setId(id);
        return role;
    }

    public Action toAction(Long id) {
        if (id == null) {
            return null;
        }
        Action action = new Action();
        action.setId(id);
        return action;
    }
}
