package dev.sentry.api.domain.access.role.api;

import dev.sentry.api.domain.access.role.Role;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    public RoleService(RoleRepository roleRepository, RoleMapper roleMapper) {
        this.roleRepository = roleRepository;
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

    /**
     * Cria um perfil de acesso garantindo que não exista outro com o mesmo nome no mesmo sistema.
     *
     * @param roleRequest dados do perfil a ser criado
     * @throws ResponseStatusException com status 409 quando já existir perfil com esse nome no sistema
     */
    public void save(RoleRest.SaveRequest roleRequest) {
        if (roleRepository.findBySystemIdAndSystemRole(roleRequest.idSystem(), roleRequest.systemRole()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um perfil com esse nome nesse sistema");
        }
        roleRepository.save(roleMapper.toEntity(roleRequest));
    }

    /**
     * Atualiza um perfil de acesso garantindo que não exista outro com o mesmo nome no mesmo sistema.
     *
     * @param id identificador do perfil
     * @param roleRequest novos dados do perfil
     * @throws ResponseStatusException com status 404 quando o perfil não existir ou 409 em caso de duplicidade
     */
    public void update(Long id, RoleRest.UpdateRequest roleRequest) {
        Role role = getOrThrow(id);
        Role sameRole = roleRepository.findBySystemIdAndSystemRole(roleRequest.idSystem(), roleRequest.systemRole());
        if (sameRole != null && !sameRole.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um perfil com esse nome nesse sistema");
        }
        roleMapper.updateEntity(roleRequest, role);
        roleRepository.save(role);
    }

    public void delete(Long id) {
        Role role = getOrThrow(id);
        role.setIsDeleted(true);
        roleRepository.save(role);
    }

    private Role getOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de acesso não encontrado"));
    }
}
