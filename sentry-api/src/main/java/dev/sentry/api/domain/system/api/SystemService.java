package dev.sentry.api.domain.system.api;

import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.ExternalSystem.ClientType;
import dev.sentry.api.domain.system.ExternalSystem.RedirectUri;
import dev.sentry.api.domain.system.ExternalSystem.UriType;
import dev.sentry.api.security.password.PasswordService;
import dev.sentry.api.utils.SortUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SystemService {

    private static final Set<String> ALLOWED_SCOPES = Set.of("openid", "profile", "email", "offline_access");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SystemRepository systemRepository;
    private final SystemMapper systemMapper;
    private final PasswordService passwordService;

    public SystemService(SystemRepository systemRepository, SystemMapper systemMapper, PasswordService passwordService) {
        this.systemRepository = systemRepository;
        this.systemMapper = systemMapper;
        this.passwordService = passwordService;
    }

    public List<SystemRest.Response> findByQueryParams(SystemRest.QueryRequest queryParams) {
        var pageable =
                PageRequest.of(queryParams.page(), queryParams.pageSize(), SortUtils.toSort(queryParams.sortBy()));
        return systemRepository.findAll(SystemSpecification.findBy(queryParams), pageable)
                .stream()
                .map(systemMapper::toDto)
                .toList();
    }

    public SystemRest.Response findById(Long id) {
        return systemMapper.toDto(getOrThrow(id));
    }

    /**
     * Cria um sistema já registrado como cliente OAuth (RN-F1-01.9). Sistemas {@code CONFIDENTIAL} recebem um
     * segredo, gravado só como hash (RN-F1-01.3).
     *
     * @param systemRequest dados do novo sistema
     * @return o {@code client_id} e, para {@code CONFIDENTIAL}, o segredo em claro, que não é mostrado de novo
     * @throws ResponseStatusException 409 se o código externo já existir; 400 se um escopo ou uma URI de
     *     redirecionamento for inválida
     */
    public SystemRest.ClientCredentials save(SystemRest.SaveRequest systemRequest) {
        if (systemRepository.findByExternalCode(systemRequest.externalCode()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um sistema com esse código externo");
        }
        validateScopes(systemRequest.scopes());
        ExternalSystem system = systemMapper.toEntity(systemRequest);
        replaceRedirectUris(system, UriType.LOGIN, systemRequest.redirectUris());
        replaceRedirectUris(system, UriType.LOGOUT, systemRequest.postLogoutRedirectUris());
        String secret = system.getClientType() == ClientType.CONFIDENTIAL ? newSecret(system) : null;
        systemRepository.save(system);
        return new SystemRest.ClientCredentials(system.getExternalCode(), secret);
    }

    /**
     * Atualiza um sistema. O código externo é o {@code client_id} e não muda (RN-F1-01.1). Campos nulos ficam
     * como estão; uma lista de URIs informada substitui as URIs daquele tipo. Ao passar para {@code PUBLIC}, o
     * segredo é descartado.
     *
     * @param id identificador interno do sistema
     * @param systemRequest dados a alterar
     * @throws ResponseStatusException 404 se o sistema não existir; 400 se um escopo ou uma URI de
     *     redirecionamento for inválida
     */
    public void update(Long id, SystemRest.UpdateRequest systemRequest) {
        ExternalSystem system = getOrThrow(id);
        validateScopes(systemRequest.scopes());
        systemMapper.updateEntity(systemRequest, system);
        replaceRedirectUris(system, UriType.LOGIN, systemRequest.redirectUris());
        replaceRedirectUris(system, UriType.LOGOUT, systemRequest.postLogoutRedirectUris());
        if (system.getClientType() == ClientType.PUBLIC) {
            system.setClientSecretHash(null);
        }
        systemRepository.save(system);
    }

    /**
     * Gera um novo segredo para um sistema {@code CONFIDENTIAL}. O hash anterior é substituído, então o segredo
     * antigo deixa de autenticar na hora (CA-F1-01.4).
     *
     * @param id identificador interno do sistema
     * @return o {@code client_id} e o novo segredo em claro, que não é mostrado de novo
     * @throws ResponseStatusException 404 se o sistema não existir; 400 se ele for {@code PUBLIC}
     */
    public SystemRest.ClientCredentials rotateClientSecret(Long id) {
        ExternalSystem system = getOrThrow(id);
        if (system.getClientType() != ClientType.CONFIDENTIAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sistema do tipo PUBLIC não tem segredo");
        }
        String secret = newSecret(system);
        systemRepository.save(system);
        return new SystemRest.ClientCredentials(system.getExternalCode(), secret);
    }

    public void delete(Long id) {
        ExternalSystem system = getOrThrow(id);
        system.setIsDeleted(true);
        systemRepository.save(system);
    }

    private ExternalSystem getOrThrow(Long id) {
        return systemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sistema não encontrado"));
    }

    private String newSecret(ExternalSystem system) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        system.setClientSecretHash(passwordService.hash(secret));
        return secret;
    }

    // RN-F1-01.6
    private static void validateScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return;
        }
        for (String scope : scopes.trim().split("\\s+")) {
            if (!ALLOWED_SCOPES.contains(scope)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Escopo não permitido: " + scope + ". Use openid, profile, email ou offline_access");
            }
        }
    }

    private static void replaceRedirectUris(ExternalSystem system, UriType type, List<String> uris) {
        if (uris == null) {
            return;
        }
        uris.forEach(SystemService::validateRedirectUri);
        system.getRedirectUris().removeIf(redirectUri -> redirectUri.type() == type);
        uris.forEach(uri -> system.getRedirectUris().add(new RedirectUri(uri, type)));
    }

    // RN-F1-01.4 e RN-F1-01.5: https, ou http só em localhost/127.0.0.1; sem fragmento
    private static void validateRedirectUri(String value) {
        URI uri = null;
        try {
            uri = value == null ? null : new URI(value);
        } catch (URISyntaxException e) {
            // tratada abaixo como URI inválida
        }
        boolean secure = uri != null && "https".equals(uri.getScheme()) && uri.getHost() != null;
        boolean local = uri != null && "http".equals(uri.getScheme())
                && ("localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
        if (!(secure || local) || uri.getFragment() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URI de redirecionamento inválida: " + value
                    + ". Use https (http só para localhost e 127.0.0.1) e não use fragmento (#)");
        }
    }
}
