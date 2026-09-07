package dev.sentry.api.domain.roleoption.api;

import dev.sentry.api.domain.roleoption.RoleOption;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoleOptionService {

    private final RoleOptionRepository roleOptionRepository;
    private final RoleOptionMapper roleOptionMapper;

    public RoleOptionService(RoleOptionRepository roleOptionRepository, RoleOptionMapper roleOptionMapper) {
        this.roleOptionRepository = roleOptionRepository;
        this.roleOptionMapper = roleOptionMapper;
    }

    public List<RoleOptionRest.Response> findByQueryParams(RoleOptionRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return roleOptionRepository.findAll(RoleOptionSpecification.findBy(queryParams), pageable)
                .stream()
                .map(roleOptionMapper::toDto)
                .toList();
    }

    public RoleOptionRest.Response findById(Long id) {
        return roleOptionMapper.toDto(getOrThrow(id));
    }

    public void save(RoleOptionRest.SaveRequest roleOptionRequest) {
        if (roleOptionRepository.findByIdRoleAndCdAction(roleOptionRequest.idRole(), roleOptionRequest.cdAction())
                != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa ação já está vinculada ao perfil");
        }
        roleOptionRepository.save(roleOptionMapper.toEntity(roleOptionRequest));
    }

    public void update(Long id, RoleOptionRest.UpdateRequest roleOptionRequest) {
        RoleOption roleOption = getOrThrow(id);
        RoleOption sameOption =
                roleOptionRepository.findByIdRoleAndCdAction(roleOptionRequest.idRole(), roleOptionRequest.cdAction());
        if (sameOption != null && !sameOption.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa ação já está vinculada ao perfil");
        }
        roleOptionMapper.updateEntity(roleOptionRequest, roleOption);
        roleOptionRepository.save(roleOption);
    }

    public void delete(Long id) {
        RoleOption roleOption = getOrThrow(id);
        roleOption.setIsDeleted(true);
        roleOptionRepository.save(roleOption);
    }

    private RoleOption getOrThrow(Long id) {
        return roleOptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opção de perfil não encontrada"));
    }
}
