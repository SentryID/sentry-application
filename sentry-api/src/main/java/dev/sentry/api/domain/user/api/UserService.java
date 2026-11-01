package dev.sentry.api.domain.user.api;

import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.User.AuthenticationType;
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
     * Cria um usuário. A senha de usuários {@code DATABASE} é gravada com hash Argon2id (RN-F0-02.7); usuários
     * {@code LDAP} não têm senha no Sentry, que é conferida no servidor LDAP (RN-F1-02.7).
     *
     * @param userRequest dados do novo usuário, com a senha em claro quando não é {@code LDAP}
     * @throws ResponseStatusException 400 se vier senha para um usuário {@code LDAP} ou faltar a senha dos demais; 409
     *     se o login (sem diferenciar maiúsculas, RN-F1-02.1) ou o e-mail já estiverem em uso
     */
    public void save(UserRest.SaveRequest userRequest) {
        boolean ldap = userRequest.authenticationType() == AuthenticationType.LDAP;
        boolean hasPassword = userRequest.password() != null && !userRequest.password().isBlank();
        if (ldap && hasPassword) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuários LDAP não têm senha no Sentry");
        }
        if (!ldap && !hasPassword) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha obrigatória para usuários DATABASE");
        }
        if (userRepository.findByUsernameIgnoreCase(userRequest.username()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse login");
        }
        if (userRepository.findByEmail(userRequest.email()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse e-mail");
        }
        User user = userMapper.toEntity(userRequest);
        user.setPassword(ldap ? null : passwordService.hash(userRequest.password()));
        userRepository.save(user);
    }

    /**
     * Atualiza um usuário. Quando ele passa a ser {@code LDAP}, o hash da senha antiga é apagado (RN-F1-02.7).
     *
     * @param id identificador interno do usuário
     * @param userRequest novos dados; campos nulos não são alterados
     * @throws ResponseStatusException 404 se o usuário não existir; 409 se o e-mail já for de outro usuário
     */
    public void update(Long id, UserRest.UpdateRequest userRequest) {
        User user = getOrThrow(id);
        User sameEmail = userRepository.findByEmail(userRequest.email());
        if (sameEmail != null && !sameEmail.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um usuário com esse e-mail");
        }
        userMapper.updateEntity(userRequest, user);
        if (user.getAuthenticationType() == AuthenticationType.LDAP) {
            user.setPassword(null);
        }
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
