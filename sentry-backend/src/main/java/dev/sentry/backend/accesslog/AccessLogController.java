package dev.sentry.backend.accesslog;

import dev.sentry.api.domain.accesslog.api.AccessLogRest;
import dev.sentry.api.domain.accesslog.api.AccessLogService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccessLogController implements AccessLogControllerSwagger {

    private final AccessLogService accessLogService;

    public AccessLogController(AccessLogService accessLogService) {
        this.accessLogService = accessLogService;
    }

    @Override
    public List<AccessLogRest.Response> findByQueryParams(AccessLogRest.QueryRequest queryParams) {
        return accessLogService.findByQueryParams(queryParams);
    }

    @Override
    public AccessLogRest.Response findById(Long id) {
        return accessLogService.findById(id);
    }

    @Override
    public void save(AccessLogRest.SaveRequest request) {
        accessLogService.save(request);
    }

    @Override
    public void update(Long id, AccessLogRest.UpdateRequest request) {
        accessLogService.update(id, request);
    }

    @Override
    public void delete(Long id) {
        accessLogService.delete(id);
    }
}
