package dev.sentry.api.domain.system.api;

import dev.sentry.api.domain.system.System;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SystemService {

    private final SystemRepository systemRepository;
    private final SystemMapper systemMapper;

    public SystemService(SystemRepository systemRepository, SystemMapper systemMapper) {
        this.systemRepository = systemRepository;
        this.systemMapper = systemMapper;
    }

    public List<SystemRest.Response> findByQueryParams(SystemRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return systemRepository.findAll(SystemSpecification.findBy(queryParams), pageable)
                .stream()
                .map(systemMapper::toDto)
                .toList();
    }

    public SystemRest.Response findById(Long id) {
        return systemMapper.toDto(getOrThrow(id));
    }

    public void save(SystemRest.SaveRequest systemRequest) {
        if (systemRequest.externalCode() != null
                && systemRepository.findByExternalCode(systemRequest.externalCode()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um sistema com esse código externo");
        }
        systemRepository.save(System.create(systemRequest.idOrganization(), systemRequest.externalCode(),
                systemRequest.description(), systemRequest.url(), systemRequest.scopes(), systemRequest.isActive()));
    }

    public void update(Long id, SystemRest.UpdateRequest systemRequest) {
        System system = getOrThrow(id);
        if (systemRequest.externalCode() != null) {
            System sameCode = systemRepository.findByExternalCode(systemRequest.externalCode());
            if (sameCode != null && !sameCode.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um sistema com esse código externo");
            }
        }
        system.update(systemRequest.idOrganization(), systemRequest.externalCode(), systemRequest.description(),
                systemRequest.url(), systemRequest.scopes(), systemRequest.isActive());
        systemRepository.save(system);
    }

    public void delete(Long id) {
        System system = getOrThrow(id);
        system.delete();
        systemRepository.save(system);
    }

    private System getOrThrow(Long id) {
        return systemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sistema não encontrado"));
    }
}
