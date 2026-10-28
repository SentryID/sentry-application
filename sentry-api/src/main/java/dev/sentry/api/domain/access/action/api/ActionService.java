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

    /**
     * Busca uma ação pelo seu código.
     *
     * @param code código da ação (ex.: {@code SENTRY.USER.VIEW})
     * @return dados da ação
     * @throws ResponseStatusException com status 404 quando a ação não existir
     */
    public ActionRest.Response findByCode(String code) {
        return actionMapper.toDto(getOrThrow(code));
    }

    /**
     * Cria uma ação garantindo que o código seja único e que não exista outra do mesmo tipo no mesmo sistema.
     *
     * @param actionRequest dados da ação a ser criada
     * @throws ResponseStatusException com status 409 quando o código já existir ou quando já existir ação do mesmo
     *     tipo no sistema
     */
    public void save(ActionRest.SaveRequest actionRequest) {
        // o código é a chave primária: sem essa checagem o save do JPA sobrescreveria a ação existente
        if (actionRepository.existsById(actionRequest.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma ação com esse código");
        }
        if (actionRepository.findBySystemIdAndActionType(actionRequest.idSystem(), actionRequest.actionType()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma ação desse tipo nesse sistema");
        }
        actionRepository.save(actionMapper.toEntity(actionRequest));
    }

    /**
     * Atualiza uma ação existente garantindo que não exista outra do mesmo tipo no mesmo sistema. O código da ação
     * é imutável.
     *
     * @param code código da ação
     * @param actionRequest novos dados da ação
     * @throws ResponseStatusException com status 404 quando a ação não existir ou 409 em caso de duplicidade
     */
    public void update(String code, ActionRest.UpdateRequest actionRequest) {
        Action action = getOrThrow(code);
        Action sameType =
                actionRepository.findBySystemIdAndActionType(actionRequest.idSystem(), actionRequest.actionType());
        if (sameType != null && !sameType.getCode().equals(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma ação desse tipo nesse sistema");
        }
        actionMapper.updateEntity(actionRequest, action);
        actionRepository.save(action);
    }

    /**
     * Exclui logicamente uma ação.
     *
     * @param code código da ação
     * @throws ResponseStatusException com status 404 quando a ação não existir
     */
    public void delete(String code) {
        Action action = getOrThrow(code);
        action.setIsDeleted(true);
        actionRepository.save(action);
    }

    private Action getOrThrow(String code) {
        return actionRepository.findById(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ação não encontrada"));
    }
}
