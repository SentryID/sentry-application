package dev.sentry.api.domain.user.api;

import dev.sentry.api.domain.user.User;
import dev.sentry.api.security.password.PasswordService;
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
    private final PasswordService passwordService;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordService = passwordService;
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

    /**
     * Cria um usuário, gravando a senha com hash Argon2id (RN-F0-02.7).
     *
     * @param userRequest dados do novo usuário, com a senha em claro
     * @throws ResponseStatusException 409 se o login (sem diferenciar maiúsculas, RN-F1-02.1) ou o e-mail já estiverem
     *     em uso
     */
    public void save(UserRest.SaveRequest userRequest) {
        if (userRepository.findByUsernameIgnoreCase(userRequest.username()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse login");
        }
        if (userRepository.findByEmail(userRequest.email()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse e-mail");
        }
        User user = userMapper.toEntity(userRequest);
        user.setPassword(passwordService.hash(userRequest.password()));
        userRepository.save(user);
    }

    public void update(Long id, UserRest.UpdateRequest userRequest) {
        User user = getOrThrow(id);
        User sameEmail = userRepository.findByEmail(userRequest.email());
        if (sameEmail != null && !sameEmail.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse e-mail");
        }
        userMapper.updateEntity(userRequest, user);
        userRepository.save(user);
    }

    public void delete(Long id) {
        User user = getOrThrow(id);
        user.setIsDeleted(true);
        userRepository.save(user);
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }
}
