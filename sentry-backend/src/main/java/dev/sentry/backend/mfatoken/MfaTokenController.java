package dev.sentry.backend.mfatoken;

import dev.sentry.api.domain.mfatoken.api.MfaTokenRest;
import dev.sentry.api.domain.mfatoken.api.MfaTokenService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MfaTokenController implements MfaTokenControllerSwagger {

    private final MfaTokenService mfaTokenService;

    public MfaTokenController(MfaTokenService mfaTokenService) {
        this.mfaTokenService = mfaTokenService;
    }

    @Override
    public List<MfaTokenRest.Response> findByQueryParams(MfaTokenRest.QueryRequest queryParams) {
        return mfaTokenService.findByQueryParams(queryParams);
    }

    @Override
    public MfaTokenRest.Response findById(Long id) {
        return mfaTokenService.findById(id);
    }

    @Override
    public void save(MfaTokenRest.SaveRequest request) {
        mfaTokenService.save(request);
    }

    @Override
    public void update(Long id, MfaTokenRest.UpdateRequest request) {
        mfaTokenService.update(id, request);
    }

    @Override
    public void delete(Long id) {
        mfaTokenService.delete(id);
    }
}
