package dev.sentry.api.domain.access.role.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.access.role.Role;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RoleServiceTest {

    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final RoleMapper roleMapper = mock(RoleMapper.class);
    private final RoleService roleService = new RoleService(roleRepository, roleMapper);

    private static RoleRest.SaveRequest saveRequest() {
        return new RoleRest.SaveRequest(1L, 1L, "ADMIN", "ADMINISTRADOR", "INTERNAL", true);
    }

    private static RoleRest.UpdateRequest updateRequest() {
        return new RoleRest.UpdateRequest(1L, 1L, "ADMIN", "ADMINISTRADOR", "INTERNAL", true);
    }

    private static Role role(Long id) {
        Role role = new Role();
        role.setId(id);
        return role;
    }

    @Test
    void saveRejectsSameRoleNameInSameSystem() {
        when(roleRepository.findBySystemIdAndSystemRole(1L, "ADMIN")).thenReturn(role(9L));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> roleService.save(saveRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(roleRepository, never()).save(any());
    }

    @Test
    void savePersistsWhenThereIsNoConflict() {
        Role mapped = role(null);
        when(roleRepository.findBySystemIdAndSystemRole(1L, "ADMIN")).thenReturn(null);
        when(roleMapper.toEntity(any(RoleRest.SaveRequest.class))).thenReturn(mapped);

        roleService.save(saveRequest());

        verify(roleRepository).save(mapped);
    }

    @Test
    void updateRejectsSameRoleNameOfAnotherRole() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L)));
        when(roleRepository.findBySystemIdAndSystemRole(1L, "ADMIN")).thenReturn(role(2L));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> roleService.update(1L, updateRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(roleRepository, never()).save(any());
    }

    @Test
    void updatePersistsWhenTheOnlyMatchIsTheRoleItself() {
        Role existing = role(1L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roleRepository.findBySystemIdAndSystemRole(1L, "ADMIN")).thenReturn(existing);

        roleService.update(1L, updateRequest());

        verify(roleMapper).updateEntity(any(RoleRest.UpdateRequest.class), any(Role.class));
        verify(roleRepository).save(existing);
    }

    @Test
    void updateFailsWhenRoleDoesNotExist() {
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> roleService.update(1L, updateRequest()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
