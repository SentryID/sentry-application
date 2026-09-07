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
    public MfaTokenRest.Response issue(MfaTokenRest.SaveRequest request) {
        return mfaTokenService.issue(request);
    }

    @Override
    public void consume(Long id) {
        mfaTokenService.consume(id);
    }

    @Override
    public void delete(Long id) {
        mfaTokenService.delete(id);
    }
}
