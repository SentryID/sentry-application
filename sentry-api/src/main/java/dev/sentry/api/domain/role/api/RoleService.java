package dev.sentry.api.domain.role.api;

import dev.sentry.api.domain.action.Action;
import dev.sentry.api.domain.action.api.ActionRepository;
import dev.sentry.api.domain.role.Role;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final ActionRepository actionRepository;
    private final RoleMapper roleMapper;

    public RoleService(RoleRepository roleRepository, ActionRepository actionRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
        this.actionRepository = actionRepository;
        this.roleMapper = roleMapper;
    }

    public List<RoleRest.Response> findByQueryParams(RoleRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return roleRepository.findAll(RoleSpecification.findBy(queryParams), pageable)
                .stream()
                .map(roleMapper::toDto)
                .toList();
    }

    public RoleRest.Response findById(Long id) {
        return roleMapper.toDto(getOrThrow(id));
    }

    public void save(RoleRest.SaveRequest roleRequest) {
        if (roleRepository.findByIdSystemAndSystemRole(roleRequest.idSystem(), roleRequest.systemRole()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um perfil com esse nome nesse sistema");
        }
        roleRepository.save(Role.create(roleRequest.idSystem(), roleRequest.idOrganization(),
                roleRequest.systemRole(), roleRequest.externalRole(), roleRequest.roleType(), roleRequest.isActive()));
    }

    public void update(Long id, RoleRest.UpdateRequest roleRequest) {
        Role role = getOrThrow(id);
        Role sameRole = roleRepository.findByIdSystemAndSystemRole(roleRequest.idSystem(), roleRequest.systemRole());
        if (sameRole != null && !sameRole.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um perfil com esse nome nesse sistema");
        }
        role.update(roleRequest.idSystem(), roleRequest.idOrganization(), roleRequest.systemRole(),
                roleRequest.externalRole(), roleRequest.roleType(), roleRequest.isActive());
        roleRepository.save(role);
    }

    public void delete(Long id) {
        Role role = getOrThrow(id);
        role.delete();
        roleRepository.save(role);
    }

    // As três abaixo tocam a coleção lazy do agregado, por isso a transação explícita.

    @Transactional(readOnly = true)
    public List<RoleRest.OptionResponse> findOptions(Long idRole) {
        return roleMapper.toOptionDtos(getOrThrow(idRole).getActiveOptions());
    }

    @Transactional
    public void grantOption(Long idRole, RoleRest.GrantRequest request) {
        Role role = getOrThrow(idRole);
        Action action = actionRepository.findById(request.cdAction())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ação não encontrada"));
        role.grant(action);
        roleRepository.save(role);
    }

    @Transactional
    public void revokeOption(Long idRole, Long cdAction) {
        Role role = getOrThrow(idRole);
        role.revoke(cdAction);
        roleRepository.save(role);
    }

    private Role getOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de acesso não encontrado"));
    }
}
