package dev.sentry.auth.oauth;

import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.ExternalSystem.ClientType;
import dev.sentry.api.domain.system.ExternalSystem.RedirectUri;
import dev.sentry.api.domain.system.ExternalSystem.UriType;
import dev.sentry.api.domain.system.api.SystemRepository;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

/**
 * Clientes OAuth lidos da tabela {@code systems} (F1-01), no lugar da {@code oauth2_registered_client} do Spring
 * Authorization Server. O {@code client_id} e o id do cliente são o {@code external_code} do sistema.
 */
@Component
public class SystemRegisteredClientRepository implements RegisteredClientRepository {

    private static final Duration AUTHORIZATION_CODE_TTL = Duration.ofSeconds(60);

    private final SystemRepository systemRepository;
    private final TokenProperties tokenProperties;

    public SystemRegisteredClientRepository(SystemRepository systemRepository, TokenProperties tokenProperties) {
        this.systemRepository = systemRepository;
        this.tokenProperties = tokenProperties;
    }

    /** Clientes são cadastrados pelo {@code sentry-backend}, nunca pelo Authorization Server. */
    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("Clientes OAuth são cadastrados pelo sentry-backend");
    }

    @Override
    public RegisteredClient findById(String id) {
        return findByClientId(id);
    }

    /**
     * Monta o cliente a partir do sistema. Sistema inexistente, desativado, excluído ou de organização inativa não
     * é cliente: o fluxo responde {@code invalid_client} (RN-F1-01.8, CA-F1-01.3).
     *
     * @param clientId {@code external_code} do sistema
     * @return o cliente, ou {@code null} se o sistema não puder autenticar
     */
    @Override
    public RegisteredClient findByClientId(String clientId) {
        ExternalSystem system = systemRepository.findByExternalCode(clientId);
        if (system == null || !system.getIsActive() || system.getIsDeleted()
                || !system.getOrganization().getIsActive() || system.getOrganization().getIsDeleted()) {
            return null;
        }
        return toRegisteredClient(system);
    }

    private RegisteredClient toRegisteredClient(ExternalSystem system) {
        boolean confidential = system.getClientType() == ClientType.CONFIDENTIAL;
        RegisteredClient.Builder builder = RegisteredClient.withId(system.getExternalCode())
                .clientId(system.getExternalCode())
                .clientName(system.getDescription())
                .clientSecret(confidential ? system.getClientSecretHash() : null)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                // RN-F1-03.2 e RN-F1-03.9: PKCE sempre, sem tela de consentimento
                .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(false).build())
                .tokenSettings(TokenSettings.builder()
                        .authorizationCodeTimeToLive(AUTHORIZATION_CODE_TTL)
                        .accessTokenTimeToLive(tokenProperties.accessTokenTtl())
                        .refreshTokenTimeToLive(tokenProperties.refreshTokenTtl())
                        .reuseRefreshTokens(false)
                        .idTokenSignatureAlgorithm(SignatureAlgorithm.RS256)
                        .build());
        if (confidential) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        } else {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        }
        for (RedirectUri redirectUri : system.getRedirectUris()) {
            if (redirectUri.type() == UriType.LOGIN) {
                builder.redirectUri(redirectUri.uri());
            } else {
                builder.postLogoutRedirectUri(redirectUri.uri());
            }
        }
        // sem URI de login não há para onde mandar o código: o sistema só usa o login direto e o refresh
        if (system.getRedirectUris().stream().anyMatch(uri -> uri.type() == UriType.LOGIN)) {
            builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
        }
        if (system.getScopes() != null && !system.getScopes().isBlank()) {
            builder.scopes(scopes -> scopes.addAll(Arrays.asList(system.getScopes().trim().split("\\s+"))));
        }
        return builder.build();
    }
}
