package dev.sentry.api.domain.mfatoken.api;

import dev.sentry.api.domain.mfatoken.MfaToken;
import dev.sentry.api.utils.SortUtils;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MfaTokenService {

    private final MfaTokenRepository mfaTokenRepository;
    private final MfaTokenMapper mfaTokenMapper;

    public MfaTokenService(MfaTokenRepository mfaTokenRepository, MfaTokenMapper mfaTokenMapper) {
        this.mfaTokenRepository = mfaTokenRepository;
        this.mfaTokenMapper = mfaTokenMapper;
    }

    public List<MfaTokenRest.Response> findByQueryParams(MfaTokenRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return mfaTokenRepository.findAll(MfaTokenSpecification.findBy(queryParams), pageable)
                .stream()
                .map(mfaTokenMapper::toDto)
                .toList();
    }

    public MfaTokenRest.Response findById(Long id) {
        return mfaTokenMapper.toDto(getOrThrow(id));
    }

    /** Devolve o token gerado — é a única chance de o chamador conhecer o valor. */
    public MfaTokenRest.Response issue(MfaTokenRest.SaveRequest mfaTokenRequest) {
        MfaToken mfaToken = MfaToken.issue(mfaTokenRequest.username(), mfaTokenRequest.code(),
                mfaTokenRequest.validUntil());
        return mfaTokenMapper.toDto(mfaTokenRepository.save(mfaToken));
    }

    /** Uso único: a segunda chamada devolve 409, e um token vencido devolve 410. */
    public void consume(Long id) {
        MfaToken mfaToken = getOrThrow(id);
        mfaToken.consume(LocalDateTime.now());
        mfaTokenRepository.save(mfaToken);
    }

    /** A tabela não tem exclusão lógica: o token é removido de fato. */
    public void delete(Long id) {
        mfaTokenRepository.delete(getOrThrow(id));
    }

    private MfaToken getOrThrow(Long id) {
        return mfaTokenRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token de MFA não encontrado"));
    }
}
