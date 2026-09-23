package dev.sentry.api.domain.access.roleuser.api;

import dev.sentry.api.domain.access.roleuser.RoleUser;
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

    /**
     * Cria o vínculo entre perfil de acesso e usuário garantindo que o usuário ainda não esteja vinculado ao perfil.
     *
     * @param roleUserRequest dados do vínculo a ser criado
     * @throws ResponseStatusException com status 409 quando o usuário já estiver vinculado ao perfil
     */
    public void save(RoleUserRest.SaveRequest roleUserRequest) {
        if (roleUserRepository.findByRoleIdAndUserId(roleUserRequest.idRole(), roleUserRequest.idUser()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse usuário já está vinculado a esse perfil");
        }
        roleUserRepository.save(roleUserMapper.toEntity(roleUserRequest));
    }

    /**
     * Atualiza o vínculo entre perfil de acesso e usuário garantindo que o usuário ainda não esteja vinculado ao perfil.
     *
     * @param id identificador do vínculo
     * @param roleUserRequest novos dados do vínculo
     * @throws ResponseStatusException com status 404 quando o vínculo não existir ou 409 em caso de duplicidade
     */
    public void update(Long id, RoleUserRest.UpdateRequest roleUserRequest) {
        RoleUser roleUser = getOrThrow(id);
        RoleUser sameLink =
                roleUserRepository.findByRoleIdAndUserId(roleUserRequest.idRole(), roleUserRequest.idUser());
        if (sameLink != null && !sameLink.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse usuário já está vinculado a esse perfil");
        }
        roleUserMapper.updateEntity(roleUserRequest, roleUser);
        roleUserRepository.save(roleUser);
    }

    public void delete(Long id) {
        RoleUser roleUser = getOrThrow(id);
        roleUser.setIsDeleted(true);
        roleUserRepository.save(roleUser);
    }

    private RoleUser getOrThrow(Long id) {
        return roleUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vínculo não encontrado"));
    }
}
