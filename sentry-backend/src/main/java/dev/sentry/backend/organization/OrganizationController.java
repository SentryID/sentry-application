package dev.sentry.backend.organization;

import dev.sentry.api.domain.organization.api.OrganizationRest;
import dev.sentry.api.domain.organization.api.OrganizationService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrganizationController implements OrganizationControllerSwagger {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @Override
    public List<OrganizationRest.Response> findByQueryParams(OrganizationRest.QueryRequest queryParams) {
        return organizationService.findByQueryParams(queryParams);
    }

    @Override
    public OrganizationRest.Response findById(Long id) {
        return organizationService.findById(id);
    }

    @Override
    public void save(OrganizationRest.SaveRequest request) {
        organizationService.save(request);
    }

    @Override
    public void update(Long id, OrganizationRest.UpdateRequest request) {
        organizationService.update(id, request);
    }

    @Override
    public void delete(Long id) {
        organizationService.delete(id);
    }
}
