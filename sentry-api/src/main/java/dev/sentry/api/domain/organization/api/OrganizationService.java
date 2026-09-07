package dev.sentry.api.domain.organization.api;

import dev.sentry.api.domain.organization.Organization;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    public OrganizationService(OrganizationRepository organizationRepository, OrganizationMapper organizationMapper) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
    }

    public List<OrganizationRest.Response> findByQueryParams(OrganizationRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return organizationRepository.findAll(OrganizationSpecification.findBy(queryParams), pageable)
                .stream()
                .map(organizationMapper::toDto)
                .toList();
    }

    public OrganizationRest.Response findById(Long id) {
        return organizationMapper.toDto(getOrThrow(id));
    }

    public void save(OrganizationRest.SaveRequest organizationRequest) {
        if (organizationRequest.externalCode() != null
                && organizationRepository.findByExternalCode(organizationRequest.externalCode()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma organização com esse código externo");
        }
        organizationRepository.save(organizationMapper.toEntity(organizationRequest));
    }

    public void update(Long id, OrganizationRest.UpdateRequest organizationRequest) {
        Organization organization = getOrThrow(id);
        organizationMapper.updateEntity(organizationRequest, organization);
        organizationRepository.save(organization);
    }

    public void delete(Long id) {
        Organization organization = getOrThrow(id);
        organization.setIsDeleted(true);
        organizationRepository.save(organization);
    }

    private Organization getOrThrow(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organização não encontrada"));
    }
}
