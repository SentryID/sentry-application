package dev.sentry.api.domain.accesslog.api;

import dev.sentry.api.domain.accesslog.AccessLog;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;
    private final AccessLogMapper accessLogMapper;

    public AccessLogService(AccessLogRepository accessLogRepository, AccessLogMapper accessLogMapper) {
        this.accessLogRepository = accessLogRepository;
        this.accessLogMapper = accessLogMapper;
    }

    public List<AccessLogRest.Response> findByQueryParams(AccessLogRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return accessLogRepository.findAll(AccessLogSpecification.findBy(queryParams), pageable)
                .stream()
                .map(accessLogMapper::toDto)
                .toList();
    }

    public AccessLogRest.Response findById(Long id) {
        return accessLogMapper.toDto(getOrThrow(id));
    }

    public void save(AccessLogRest.SaveRequest accessLogRequest) {
        accessLogRepository.save(accessLogMapper.toEntity(accessLogRequest));
    }

    public void update(Long id, AccessLogRest.UpdateRequest accessLogRequest) {
        AccessLog accessLog = getOrThrow(id);
        accessLogMapper.updateEntity(accessLogRequest, accessLog);
        accessLogRepository.save(accessLog);
    }

    /** A tabela não tem exclusão lógica: o log é removido de fato. */
    public void delete(Long id) {
        accessLogRepository.delete(getOrThrow(id));
    }

    private AccessLog getOrThrow(Long id) {
        return accessLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log de acesso não encontrado"));
    }
}
