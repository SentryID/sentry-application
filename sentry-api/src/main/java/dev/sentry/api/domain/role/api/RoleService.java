package dev.sentry.api.domain.role.api;

import dev.sentry.api.domain.role.Role;
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

    public void save(RoleRest.SaveRequest roleRequest) {
        if (roleRepository.findByIdSystemAndSystemRole(roleRequest.idSystem(), roleRequest.systemRole()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um perfil com esse nome nesse sistema");
        }
        roleRepository.save(roleMapper.toEntity(roleRequest));
    }

    public void update(Long id, RoleRest.UpdateRequest roleRequest) {
        Role role = getOrThrow(id);
        Role sameRole = roleRepository.findByIdSystemAndSystemRole(roleRequest.idSystem(), roleRequest.systemRole());
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
