package dev.sentry.api.domain.roleuser.api;

import dev.sentry.api.domain.roleuser.RoleUser;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoleUserService {

    private final RoleUserRepository roleUserRepository;
    private final RoleUserMapper roleUserMapper;

    public RoleUserService(RoleUserRepository roleUserRepository, RoleUserMapper roleUserMapper) {
        this.roleUserRepository = roleUserRepository;
        this.roleUserMapper = roleUserMapper;
    }

    public List<RoleUserRest.Response> findByQueryParams(RoleUserRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return roleUserRepository.findAll(RoleUserSpecification.findBy(queryParams), pageable)
                .stream()
                .map(roleUserMapper::toDto)
                .toList();
    }

    public RoleUserRest.Response findById(Long id) {
        return roleUserMapper.toDto(getOrThrow(id));
    }

    public void save(RoleUserRest.SaveRequest roleUserRequest) {
        if (roleUserRepository.findByIdGroupAndIdUser(roleUserRequest.idGroup(), roleUserRequest.idUser()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse usuário já está vinculado a esse grupo");
        }
        roleUserRepository.save(
                RoleUser.assign(roleUserRequest.idGroup(), roleUserRequest.idUser(), roleUserRequest.isActive()));
    }

    public void update(Long id, RoleUserRest.UpdateRequest roleUserRequest) {
        RoleUser roleUser = getOrThrow(id);
        RoleUser sameLink =
                roleUserRepository.findByIdGroupAndIdUser(roleUserRequest.idGroup(), roleUserRequest.idUser());
        if (sameLink != null && !sameLink.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse usuário já está vinculado a esse grupo");
        }
        roleUser.update(roleUserRequest.idGroup(), roleUserRequest.idUser(), roleUserRequest.isActive());
        roleUserRepository.save(roleUser);
    }

    public void delete(Long id) {
        RoleUser roleUser = getOrThrow(id);
        roleUser.delete();
        roleUserRepository.save(roleUser);
    }

    private RoleUser getOrThrow(Long id) {
        return roleUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado"));
    }
}
