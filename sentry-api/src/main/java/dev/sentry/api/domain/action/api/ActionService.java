package dev.sentry.api.domain.action.api;

import dev.sentry.api.domain.action.Action;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActionService {

    private final ActionRepository actionRepository;
    private final ActionMapper actionMapper;

    public ActionService(ActionRepository actionRepository, ActionMapper actionMapper) {
        this.actionRepository = actionRepository;
        this.actionMapper = actionMapper;
    }

    public List<ActionRest.Response> findByQueryParams(ActionRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return actionRepository.findAll(ActionSpecification.findBy(queryParams), pageable)
                .stream()
                .map(actionMapper::toDto)
                .toList();
    }

    public ActionRest.Response findById(Long id) {
        return actionMapper.toDto(getOrThrow(id));
    }

    public void save(ActionRest.SaveRequest actionRequest) {
        if (actionRepository.findByIdSystemAndActionType(actionRequest.idSystem(), actionRequest.actionType()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma ação desse tipo nesse sistema");
        }
        actionRepository.save(actionMapper.toEntity(actionRequest));
    }

    public void update(Long id, ActionRest.UpdateRequest actionRequest) {
        Action action = getOrThrow(id);
        Action sameType =
                actionRepository.findByIdSystemAndActionType(actionRequest.idSystem(), actionRequest.actionType());
        if (sameType != null && !sameType.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma ação desse tipo nesse sistema");
        }
        actionMapper.updateEntity(actionRequest, action);
        actionRepository.save(action);
    }

    public void delete(Long id) {
        Action action = getOrThrow(id);
        action.setIsDeleted(true);
        actionRepository.save(action);
    }

    private Action getOrThrow(Long id) {
        return actionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ação não encontrada"));
    }
}
