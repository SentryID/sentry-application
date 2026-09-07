package dev.sentry.api.domain.passwordtoken.api;

import dev.sentry.api.domain.passwordtoken.PasswordToken;
import dev.sentry.api.utils.SortUtils;
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

    public void save(PasswordTokenRest.SaveRequest passwordTokenRequest) {
        if (passwordTokenRepository.findByToken(passwordTokenRequest.token()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um token de senha com esse valor");
        }
        passwordTokenRepository.save(passwordTokenMapper.toEntity(passwordTokenRequest));
    }

    public void update(Long id, PasswordTokenRest.UpdateRequest passwordTokenRequest) {
        PasswordToken passwordToken = getOrThrow(id);
        passwordTokenMapper.updateEntity(passwordTokenRequest, passwordToken);
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
