package dev.sentry.api.domain.access.action.api;

import dev.sentry.api.domain.access.action.Action;
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

    /**
     * Cria uma ação garantindo que não exista outra do mesmo tipo no mesmo sistema.
     *
     * @param actionRequest dados da ação a ser criada
     * @throws ResponseStatusException com status 409 quando já existir ação do mesmo tipo no sistema
     */
    public void save(ActionRest.SaveRequest actionRequest) {
        if (actionRepository.findBySystemIdAndActionType(actionRequest.idSystem(), actionRequest.actionType()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma ação desse tipo nesse sistema");
        }
        actionRepository.save(actionMapper.toEntity(actionRequest));
    }

    /**
     * Atualiza uma ação existente garantindo que não exista outra do mesmo tipo no mesmo sistema.
     *
     * @param id identificador da ação
     * @param actionRequest novos dados da ação
     * @throws ResponseStatusException com status 404 quando a ação não existir ou 409 em caso de duplicidade
     */
    public void update(Long id, ActionRest.UpdateRequest actionRequest) {
        Action action = getOrThrow(id);
        Action sameType =
                actionRepository.findBySystemIdAndActionType(actionRequest.idSystem(), actionRequest.actionType());
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
