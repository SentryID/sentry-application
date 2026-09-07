package dev.sentry.backend.role;

import dev.sentry.api.domain.role.api.RoleRest;
import dev.sentry.api.domain.role.api.RoleService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleOptionController implements RoleOptionControllerSwagger {

    private final RoleService roleService;

    public RoleOptionController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public List<RoleRest.OptionResponse> findOptions(Long idRole) {
        return roleService.findOptions(idRole);
    }

    @Override
    public void grantOption(Long idRole, RoleRest.GrantRequest request) {
        roleService.grantOption(idRole, request);
    }

    @Override
    public void revokeOption(Long idRole, Long cdAction) {
        roleService.revokeOption(idRole, cdAction);
    }
}
