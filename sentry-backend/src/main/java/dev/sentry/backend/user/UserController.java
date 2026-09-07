package dev.sentry.backend.user;

import dev.sentry.api.domain.user.api.UserRest;
import dev.sentry.api.domain.user.api.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements UserControllerSwagger {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public List<UserRest.Response> findByQueryParams(UserRest.QueryRequest queryParams) {
        return userService.findByQueryParams(queryParams);
    }

    @Override
    public UserRest.Response findById(Long id) {
        return userService.findById(id);
    }

    @Override
    public void save(UserRest.SaveRequest request) {
        userService.save(request);
    }

    @Override
    public void update(Long id, UserRest.UpdateRequest request) {
        userService.update(id, request);
    }

    @Override
    public void delete(Long id) {
        userService.delete(id);
    }
}
