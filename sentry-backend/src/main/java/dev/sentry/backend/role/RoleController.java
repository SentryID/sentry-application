package dev.sentry.backend.role;

import dev.sentry.api.domain.role.api.RoleRest;
import dev.sentry.api.domain.role.api.RoleService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleController implements RoleControllerSwagger {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public List<RoleRest.Response> findByQueryParams(RoleRest.QueryRequest queryParams) {
        return roleService.findByQueryParams(queryParams);
    }

    @Override
    public RoleRest.Response findById(Long id) {
        return roleService.findById(id);
    }

    @Override
    public void save(RoleRest.SaveRequest request) {
        roleService.save(request);
    }

    @Override
    public void update(Long id, RoleRest.UpdateRequest request) {
        roleService.update(id, request);
    }

    @Override
    public void delete(Long id) {
        roleService.delete(id);
    }
}
