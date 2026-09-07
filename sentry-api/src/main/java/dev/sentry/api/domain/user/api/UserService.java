package dev.sentry.api.domain.user.api;

import dev.sentry.api.domain.user.User;
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

    // ponytail: senha gravada como recebida; trocar por hash + salt quando o sentry-auth definir o algoritmo
    public void save(UserRest.SaveRequest userRequest) {
        if (userRepository.findByUsername(userRequest.username()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse login");
        }
        if (userRepository.findByEmail(userRequest.email()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse e-mail");
        }
        userRepository.save(userMapper.toEntity(userRequest));
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
