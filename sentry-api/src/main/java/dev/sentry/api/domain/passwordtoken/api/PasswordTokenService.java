package dev.sentry.api.domain.passwordtoken.api;

import dev.sentry.api.domain.passwordtoken.PasswordToken;
import dev.sentry.api.utils.SortUtils;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordTokenService {

    private final PasswordTokenRepository passwordTokenRepository;
    private final PasswordTokenMapper passwordTokenMapper;

    public PasswordTokenService(PasswordTokenRepository passwordTokenRepository,
            PasswordTokenMapper passwordTokenMapper) {
        this.passwordTokenRepository = passwordTokenRepository;
        this.passwordTokenMapper = passwordTokenMapper;
    }

    public List<PasswordTokenRest.Response> findByQueryParams(PasswordTokenRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return passwordTokenRepository.findAll(PasswordTokenSpecification.findBy(queryParams), pageable)
                .stream()
                .map(passwordTokenMapper::toDto)
                .toList();
    }

    public PasswordTokenRest.Response findById(Long id) {
        return passwordTokenMapper.toDto(getOrThrow(id));
    }

    /** Devolve o token gerado — é a única chance de o chamador conhecer o valor. */
    public PasswordTokenRest.Response issue(PasswordTokenRest.SaveRequest passwordTokenRequest) {
        PasswordToken passwordToken =
                PasswordToken.issue(passwordTokenRequest.idUser(), passwordTokenRequest.validUntil());
        return passwordTokenMapper.toDto(passwordTokenRepository.save(passwordToken));
    }

    /** Uso único: a segunda chamada devolve 409, e um token vencido devolve 410. */
    public void consume(Long id) {
        PasswordToken passwordToken = getOrThrow(id);
        passwordToken.consume(LocalDateTime.now());
        passwordTokenRepository.save(passwordToken);
    }

    /** A tabela não tem exclusão lógica: o token é removido de fato. */
    public void delete(Long id) {
        passwordTokenRepository.delete(getOrThrow(id));
    }

    private PasswordToken getOrThrow(Long id) {
        return passwordTokenRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token de senha não encontrado"));
    }
}
