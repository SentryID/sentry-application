package dev.sentry.api.domain.access.roleuser.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.access.roleuser.RoleUser;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RoleUserServiceTest {

    private final RoleUserRepository roleUserRepository = mock(RoleUserRepository.class);
    private final RoleUserMapper roleUserMapper = mock(RoleUserMapper.class);
    private final RoleUserService roleUserService = new RoleUserService(roleUserRepository, roleUserMapper);

    private static RoleUserRest.SaveRequest saveRequest() {
        return new RoleUserRest.SaveRequest(1L, 2L, true);
    }

    private static RoleUserRest.UpdateRequest updateRequest() {
        return new RoleUserRest.UpdateRequest(1L, 2L, true);
    }

    private static RoleUser roleUser(Long id) {
        RoleUser roleUser = new RoleUser();
        roleUser.setId(id);
        return roleUser;
    }

    @Test
    void saveRejectsUserAlreadyLinkedToRole() {
        when(roleUserRepository.findByRoleIdAndUserId(1L, 2L)).thenReturn(roleUser(9L));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> roleUserService.save(saveRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(roleUserRepository, never()).save(any());
    }

    @Test
    void savePersistsWhenThereIsNoConflict() {
        RoleUser mapped = roleUser(null);
        when(roleUserRepository.findByRoleIdAndUserId(1L, 2L)).thenReturn(null);
        when(roleUserMapper.toEntity(any(RoleUserRest.SaveRequest.class))).thenReturn(mapped);

        roleUserService.save(saveRequest());

        verify(roleUserRepository).save(mapped);
    }

    @Test
    void updateRejectsLinkAlreadyUsedByAnotherRecord() {
        when(roleUserRepository.findById(1L)).thenReturn(Optional.of(roleUser(1L)));
        when(roleUserRepository.findByRoleIdAndUserId(1L, 2L)).thenReturn(roleUser(2L));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> roleUserService.update(1L, updateRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(roleUserRepository, never()).save(any());
    }

    @Test
    void updatePersistsWhenTheOnlyMatchIsTheLinkItself() {
        RoleUser existing = roleUser(1L);
        when(roleUserRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roleUserRepository.findByRoleIdAndUserId(1L, 2L)).thenReturn(existing);

        roleUserService.update(1L, updateRequest());

        verify(roleUserMapper).updateEntity(any(RoleUserRest.UpdateRequest.class), any(RoleUser.class));
        verify(roleUserRepository).save(existing);
    }

    @Test
    void updateFailsWhenLinkDoesNotExist() {
        when(roleUserRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> roleUserService.update(1L, updateRequest()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
