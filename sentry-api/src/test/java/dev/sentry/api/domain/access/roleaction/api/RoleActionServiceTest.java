package dev.sentry.api.domain.access.roleaction.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.access.roleaction.RoleAction;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RoleActionServiceTest {

    private final RoleActionRepository roleActionRepository = mock(RoleActionRepository.class);
    private final RoleActionMapper roleActionMapper = mock(RoleActionMapper.class);
    private final RoleActionService roleActionService = new RoleActionService(roleActionRepository, roleActionMapper);

    private static RoleActionRest.SaveRequest saveRequest() {
        return new RoleActionRest.SaveRequest(1L, 2L, true);
    }

    private static RoleActionRest.UpdateRequest updateRequest() {
        return new RoleActionRest.UpdateRequest(1L, 2L, true);
    }

    private static RoleAction roleAction(Long id) {
        RoleAction roleAction = new RoleAction();
        roleAction.setId(id);
        return roleAction;
    }

    @Test
    void saveRejectsActionAlreadyLinkedToRole() {
        when(roleActionRepository.findByRoleIdAndActionId(1L, 2L)).thenReturn(roleAction(9L));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> roleActionService.save(saveRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(roleActionRepository, never()).save(any());
    }

    @Test
    void savePersistsWhenThereIsNoConflict() {
        RoleAction mapped = roleAction(null);
        when(roleActionRepository.findByRoleIdAndActionId(1L, 2L)).thenReturn(null);
        when(roleActionMapper.toEntity(any(RoleActionRest.SaveRequest.class))).thenReturn(mapped);

        roleActionService.save(saveRequest());

        verify(roleActionRepository).save(mapped);
    }

    @Test
    void updateRejectsLinkAlreadyUsedByAnotherRecord() {
        when(roleActionRepository.findById(1L)).thenReturn(Optional.of(roleAction(1L)));
        when(roleActionRepository.findByRoleIdAndActionId(1L, 2L)).thenReturn(roleAction(2L));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> roleActionService.update(1L, updateRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(roleActionRepository, never()).save(any());
    }

    @Test
    void updatePersistsWhenTheOnlyMatchIsTheLinkItself() {
        RoleAction existing = roleAction(1L);
        when(roleActionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(roleActionRepository.findByRoleIdAndActionId(1L, 2L)).thenReturn(existing);

        roleActionService.update(1L, updateRequest());

        verify(roleActionMapper).updateEntity(any(RoleActionRest.UpdateRequest.class), any(RoleAction.class));
        verify(roleActionRepository).save(existing);
    }

    @Test
    void updateFailsWhenLinkDoesNotExist() {
        when(roleActionRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> roleActionService.update(1L, updateRequest()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
