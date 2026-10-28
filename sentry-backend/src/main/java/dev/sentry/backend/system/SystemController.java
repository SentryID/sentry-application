package dev.sentry.backend.system;

import dev.sentry.api.domain.system.api.SystemRest;
import dev.sentry.api.domain.system.api.SystemService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController implements SystemControllerSwagger {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @Override
    public List<SystemRest.Response> findByQueryParams(SystemRest.QueryRequest queryParams) {
        return systemService.findByQueryParams(queryParams);
    }

    @Override
    public SystemRest.Response findById(Long id) {
        return systemService.findById(id);
    }

    @Override
    public void save(SystemRest.SaveRequest request) {
        systemService.save(request);
    }

    @Override
    public void update(Long id, SystemRest.UpdateRequest request) {
        systemService.update(id, request);
    }

    @Override
    public void delete(Long id) {
        systemService.delete(id);
    }
}
