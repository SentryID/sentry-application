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
    public void save(PasswordTokenRest.SaveRequest request) {
        passwordTokenService.save(request);
    }

    @Override
    public void update(Long id, PasswordTokenRest.UpdateRequest request) {
        passwordTokenService.update(id, request);
    }

    @Override
    public void delete(Long id) {
        passwordTokenService.delete(id);
    }
}
