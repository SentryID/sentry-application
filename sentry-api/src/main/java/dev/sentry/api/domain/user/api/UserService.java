package dev.sentry.api.domain.user.api;

import dev.sentry.api.domain.user.Email;
import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.Username;
import dev.sentry.api.utils.SortUtils;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public List<UserRest.Response> findByQueryParams(UserRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return userRepository.findAll(UserSpecification.findBy(queryParams), pageable)
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    public UserRest.Response findById(Long id) {
        return userMapper.toDto(getOrThrow(id));
    }

    public void save(UserRest.SaveRequest userRequest) {
        Username username = Username.of(userRequest.username());
        Email email = Email.of(userRequest.email());
        if (userRepository.findByUsername(username) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse login");
        }
        if (userRepository.findByEmail(email) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse e-mail");
        }
        userRepository.save(User.register(userRequest.name(), username, email, userRequest.authenticationType(),
                userRequest.password(), userRequest.isActive()));
    }

    public void update(Long id, UserRest.UpdateRequest userRequest) {
        User user = getOrThrow(id);
        Email email = Email.of(userRequest.email());
        User sameEmail = userRepository.findByEmail(email);
        if (sameEmail != null && !sameEmail.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse e-mail");
        }
        user.update(userRequest.name(), email, userRequest.authenticationType(), userRequest.isActive());
        userRepository.save(user);
    }

    public void delete(Long id) {
        User user = getOrThrow(id);
        user.delete();
        userRepository.save(user);
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }
}
