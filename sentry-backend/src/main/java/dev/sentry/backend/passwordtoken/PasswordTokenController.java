package dev.sentry.backend.passwordtoken;

import dev.sentry.api.domain.passwordtoken.api.PasswordTokenRest;
import dev.sentry.api.domain.passwordtoken.api.PasswordTokenService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordTokenController implements PasswordTokenControllerSwagger {

    private final PasswordTokenService passwordTokenService;

    public PasswordTokenController(PasswordTokenService passwordTokenService) {
        this.passwordTokenService = passwordTokenService;
    }

    @Override
    public List<PasswordTokenRest.Response> findByQueryParams(PasswordTokenRest.QueryRequest queryParams) {
        return passwordTokenService.findByQueryParams(queryParams);
    }

    @Override
    public PasswordTokenRest.Response findById(Long id) {
        return passwordTokenService.findById(id);
    }

    @Override
    public PasswordTokenRest.Response issue(PasswordTokenRest.SaveRequest request) {
        return passwordTokenService.issue(request);
    }

    @Override
    public void consume(Long id) {
        passwordTokenService.consume(id);
    }

    @Override
    public void delete(Long id) {
        passwordTokenService.delete(id);
    }
}
