package dev.sentry.api.domain.accesslog.api;

import dev.sentry.api.domain.accesslog.AccessLog;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessLogService {

    private static final Logger log = LoggerFactory.getLogger(AccessLogService.class);
    private static final int USER_AGENT_MAX_LENGTH = 512;

    private final AccessLogRepository accessLogRepository;
    private final AccessLogMapper accessLogMapper;
    private final TransactionTemplate newTransaction;

    public AccessLogService(AccessLogRepository accessLogRepository, AccessLogMapper accessLogMapper,
            PlatformTransactionManager transactionManager) {
        this.accessLogRepository = accessLogRepository;
        this.accessLogMapper = accessLogMapper;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Registra uma tentativa de autenticação, com sucesso ou falha (RN-F1-10.1). Grava em transação própria, para
     * que o registro fique mesmo quando o login falha, e nunca lança exceção: um erro ao gravar vira log de erro e o
     * login segue (RN-F1-10.6).
     *
     * @param accessLog tentativa a registrar; o {@code User-Agent} é cortado em 512 caracteres
     */
    public void recordAttempt(AccessLog accessLog) {
        String userAgent = accessLog.getUserAgent();
        if (userAgent != null && userAgent.length() > USER_AGENT_MAX_LENGTH) {
            accessLog.setUserAgent(userAgent.substring(0, USER_AGENT_MAX_LENGTH));
        }
        try {
            newTransaction.executeWithoutResult(status -> accessLogRepository.save(accessLog));
        } catch (RuntimeException e) {
            log.error("Falha ao gravar o registro da tentativa de autenticação (resultado {})",
                    accessLog.getAttemptResult(), e);
        }
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
