package dev.sentry.backend.roleuser;

import dev.sentry.api.domain.roleuser.api.RoleUserRest;
import dev.sentry.api.domain.roleuser.api.RoleUserService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleUserController implements RoleUserControllerSwagger {

    private final RoleUserService roleUserService;

    public RoleUserController(RoleUserService roleUserService) {
        this.roleUserService = roleUserService;
    }

    @Override
    public List<RoleUserRest.Response> findByQueryParams(RoleUserRest.QueryRequest queryParams) {
        return roleUserService.findByQueryParams(queryParams);
    }

    @Override
    public RoleUserRest.Response findById(Long id) {
        return roleUserService.findById(id);
    }

    @Override
    public void save(RoleUserRest.SaveRequest request) {
        roleUserService.save(request);
    }

    @Override
    public void update(Long id, RoleUserRest.UpdateRequest request) {
        roleUserService.update(id, request);
    }

    @Override
    public void delete(Long id) {
        roleUserService.delete(id);
    }
}
