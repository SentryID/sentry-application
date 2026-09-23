package dev.sentry.api.domain.access.roleaction.api;

import dev.sentry.api.domain.access.roleaction.RoleAction;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoleActionService {

    private final RoleActionRepository roleActionRepository;
    private final RoleActionMapper roleActionMapper;

    public RoleActionService(RoleActionRepository roleActionRepository, RoleActionMapper roleActionMapper) {
        this.roleActionRepository = roleActionRepository;
        this.roleActionMapper = roleActionMapper;
    }

    public List<RoleActionRest.Response> findByQueryParams(RoleActionRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return roleActionRepository.findAll(RoleActionSpecification.findBy(queryParams), pageable)
                .stream()
                .map(roleActionMapper::toDto)
                .toList();
    }

    public RoleActionRest.Response findById(Long id) {
        return roleActionMapper.toDto(getOrThrow(id));
    }

    /**
     * Cria o vínculo entre perfil e ação garantindo que a ação ainda não esteja vinculada ao perfil.
     *
     * @param roleActionRequest dados do vínculo a ser criado
     * @throws ResponseStatusException com status 409 quando a ação já estiver vinculada ao perfil
     */
    public void save(RoleActionRest.SaveRequest roleActionRequest) {
        if (roleActionRepository.findByRoleIdAndActionId(roleActionRequest.idRole(), roleActionRequest.cdAction())
                != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa ação já está vinculada ao perfil");
        }
        roleActionRepository.save(roleActionMapper.toEntity(roleActionRequest));
    }

    /**
     * Atualiza o vínculo entre perfil e ação garantindo que a ação ainda não esteja vinculada ao perfil.
     *
     * @param id identificador do vínculo
     * @param roleActionRequest novos dados do vínculo
     * @throws ResponseStatusException com status 404 quando o vínculo não existir ou 409 em caso de duplicidade
     */
    public void update(Long id, RoleActionRest.UpdateRequest roleActionRequest) {
        RoleAction roleAction = getOrThrow(id);
        RoleAction sameAction =
                roleActionRepository.findByRoleIdAndActionId(roleActionRequest.idRole(), roleActionRequest.cdAction());
        if (sameAction != null && !sameAction.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa ação já está vinculada ao perfil");
        }
        roleActionMapper.updateEntity(roleActionRequest, roleAction);
        roleActionRepository.save(roleAction);
    }

    public void delete(Long id) {
        RoleAction roleAction = getOrThrow(id);
        roleAction.setIsDeleted(true);
        roleActionRepository.save(roleAction);
    }

    private RoleAction getOrThrow(Long id) {
        return roleActionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opção de perfil não encontrada"));
    }
}
