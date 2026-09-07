package dev.sentry.api.domain.accesslog.api;

import dev.sentry.api.domain.accesslog.AccessLog;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Sem {@code update} nem {@code delete}: o log de acesso é append-only. */
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
        return accessLogRepository.findById(id)
                .map(accessLogMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Log de acesso não encontrado"));
    }

    public void save(AccessLogRest.SaveRequest accessLogRequest) {
        accessLogRepository.save(AccessLog.record(accessLogRequest.idUser(), accessLogRequest.idSystem(),
                accessLogRequest.attemptResult(), accessLogRequest.ipHost()));
    }
}
