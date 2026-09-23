package dev.sentry.api.domain.access.action.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.access.action.Action;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ActionServiceTest {

    private final ActionRepository actionRepository = mock(ActionRepository.class);
    private final ActionMapper actionMapper = mock(ActionMapper.class);
    private final ActionService actionService = new ActionService(actionRepository, actionMapper);

    private static ActionRest.SaveRequest saveRequest() {
        return new ActionRest.SaveRequest("LOGIN", 1L, true);
    }

    private static ActionRest.UpdateRequest updateRequest() {
        return new ActionRest.UpdateRequest("LOGIN", 1L, true);
    }

    private static Action action(Long id) {
        Action action = new Action();
        action.setId(id);
        return action;
    }

    @Test
    void saveRejectsSameActionTypeInSameSystem() {
        when(actionRepository.findBySystemIdAndActionType(1L, "LOGIN")).thenReturn(action(9L));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> actionService.save(saveRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(actionRepository, never()).save(any());
    }

    @Test
    void savePersistsWhenThereIsNoConflict() {
        Action mapped = action(null);
        when(actionRepository.findBySystemIdAndActionType(1L, "LOGIN")).thenReturn(null);
        when(actionMapper.toEntity(any(ActionRest.SaveRequest.class))).thenReturn(mapped);

        actionService.save(saveRequest());

        verify(actionRepository).save(mapped);
    }

    @Test
    void updateRejectsSameActionTypeOfAnotherAction() {
        when(actionRepository.findById(1L)).thenReturn(Optional.of(action(1L)));
        when(actionRepository.findBySystemIdAndActionType(1L, "LOGIN")).thenReturn(action(2L));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> actionService.update(1L, updateRequest()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(actionRepository, never()).save(any());
    }

    @Test
    void updatePersistsWhenTheOnlyMatchIsTheActionItself() {
        Action existing = action(1L);
        when(actionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(actionRepository.findBySystemIdAndActionType(1L, "LOGIN")).thenReturn(existing);

        actionService.update(1L, updateRequest());

        verify(actionMapper).updateEntity(any(ActionRest.UpdateRequest.class), any(Action.class));
        verify(actionRepository).save(existing);
    }

    @Test
    void updateFailsWhenActionDoesNotExist() {
        when(actionRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> actionService.update(1L, updateRequest()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
