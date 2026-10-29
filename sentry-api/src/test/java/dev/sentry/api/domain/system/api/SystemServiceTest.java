package dev.sentry.api.domain.system.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.ExternalSystem.ClientType;
import dev.sentry.api.domain.system.ExternalSystem.RedirectUri;
import dev.sentry.api.domain.system.ExternalSystem.UriType;
import dev.sentry.api.security.password.PasswordService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SystemServiceTest {

    private static final String HASH = "{argon2}$argon2id$v=19$m=19456,t=2,p=1$salt$hash";

    private final SystemRepository systemRepository = mock(SystemRepository.class);
    private final SystemMapper systemMapper = mock(SystemMapper.class);
    private final PasswordService passwordService = mock(PasswordService.class);
    private final SystemService systemService = new SystemService(systemRepository, systemMapper, passwordService);

    private static SystemRest.SaveRequest saveRequest(ClientType type, String scopes, List<String> redirectUris) {
        return new SystemRest.SaveRequest(1L, "SYS001", "Portal", null, scopes, type, redirectUris,
                List.of("https://portal.sentry.dev"), false, true);
    }

    private static SystemRest.UpdateRequest updateRequest(ClientType type, List<String> redirectUris) {
        return new SystemRest.UpdateRequest(1L, "Portal", null, null, type, redirectUris, null, null, true);
    }

    private static ExternalSystem system(ClientType type) {
        ExternalSystem system = new ExternalSystem();
        system.setId(1L);
        system.setExternalCode("SYS001");
        system.setClientType(type);
        return system;
    }

    private void mapToEntity(ClientType type) {
        when(systemMapper.toEntity(any(SystemRest.SaveRequest.class))).thenReturn(system(type));
        when(passwordService.hash(anyString())).thenReturn(HASH);
    }

    // CA-F1-01.1
    @Test
    void saveConfidentialReturnsSecretOnceAndStoresOnlyHash() {
        mapToEntity(ClientType.CONFIDENTIAL);

        SystemRest.ClientCredentials credentials = systemService.save(
                saveRequest(ClientType.CONFIDENTIAL, "openid profile", List.of("https://portal.sentry.dev/cb")));

        assertEquals("SYS001", credentials.clientId());
        assertThat(credentials.clientSecret()).hasSizeGreaterThanOrEqualTo(43);
        verify(passwordService).hash(credentials.clientSecret());
        ExternalSystem saved = captureSaved();
        assertEquals(HASH, saved.getClientSecretHash());
        assertThat(saved.getRedirectUris()).containsExactlyInAnyOrder(
                new RedirectUri("https://portal.sentry.dev/cb", UriType.LOGIN),
                new RedirectUri("https://portal.sentry.dev", UriType.LOGOUT));
        assertThat(credentials.toString()).doesNotContain(credentials.clientSecret());
    }

    @Test
    void savePublicHasNoSecret() {
        mapToEntity(ClientType.PUBLIC);

        SystemRest.ClientCredentials credentials =
                systemService.save(saveRequest(ClientType.PUBLIC, "openid", List.of("http://127.0.0.1:3000/cb")));

        assertNull(credentials.clientSecret());
        assertNull(captureSaved().getClientSecretHash());
        verify(passwordService, never()).hash(any());
    }

    @Test
    void saveRejectsDuplicatedExternalCode() {
        when(systemRepository.findByExternalCode("SYS001")).thenReturn(system(ClientType.PUBLIC));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> systemService.save(saveRequest(ClientType.PUBLIC, null, List.of())));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(systemRepository, never()).save(any());
    }

    // CA-F1-01.2 e RN-F1-01.5
    @ParameterizedTest
    @ValueSource(strings = {"http://app.exemplo.com/cb", "ftp://app.exemplo.com/cb", "/cb", "https://app.exemplo.com/cb#x",
            "http://localhost.exemplo.com/cb", "http://localhost:3000/cb", "nao é uri"})
    void saveRejectsInvalidRedirectUri(String uri) {
        mapToEntity(ClientType.PUBLIC);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> systemService.save(saveRequest(ClientType.PUBLIC, null, List.of(uri))));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertThat(exception.getReason()).startsWith("URI de redirecionamento inválida: " + uri);
        verify(systemRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://app.exemplo.com/cb", "http://127.0.0.1:3000/cb", "http://127.0.0.1/cb"})
    void saveAcceptsHttpsAndLocalRedirectUri(String uri) {
        mapToEntity(ClientType.PUBLIC);

        systemService.save(saveRequest(ClientType.PUBLIC, null, List.of(uri)));

        assertThat(captureSaved().getRedirectUris()).contains(new RedirectUri(uri, UriType.LOGIN));
    }

    // RN-F1-01.6
    @Test
    void saveRejectsScopeOutsidePhaseOne() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> systemService.save(saveRequest(ClientType.PUBLIC, "openid write", List.of())));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertThat(exception.getReason()).contains("write");
        verify(systemRepository, never()).save(any());
    }

    // CA-F1-01.4
    @Test
    void rotateClientSecretReplacesHash() {
        ExternalSystem system = system(ClientType.CONFIDENTIAL);
        system.setClientSecretHash("{argon2}old");
        when(systemRepository.findById(1L)).thenReturn(Optional.of(system));
        when(passwordService.hash(anyString())).thenReturn(HASH);

        SystemRest.ClientCredentials credentials = systemService.rotateClientSecret(1L);

        verify(passwordService).hash(credentials.clientSecret());
        assertEquals(HASH, captureSaved().getClientSecretHash());
    }

    @Test
    void rotateClientSecretRejectsPublicSystem() {
        when(systemRepository.findById(1L)).thenReturn(Optional.of(system(ClientType.PUBLIC)));

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> systemService.rotateClientSecret(1L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(systemRepository, never()).save(any());
    }

    @Test
    void updateToPublicDropsSecret() {
        ExternalSystem system = system(ClientType.CONFIDENTIAL);
        system.setClientSecretHash(HASH);
        when(systemRepository.findById(1L)).thenReturn(Optional.of(system));
        // o mapper real copia o tipo do request para a entidade
        doAnswer(invocation -> {
            system.setClientType(ClientType.PUBLIC);
            return null;
        }).when(systemMapper).updateEntity(any(), any());

        systemService.update(1L, updateRequest(ClientType.PUBLIC, null));

        assertNull(captureSaved().getClientSecretHash());
    }

    @Test
    void updateReplacesOnlyInformedUriType() {
        ExternalSystem system = system(ClientType.PUBLIC);
        system.getRedirectUris().add(new RedirectUri("https://old.sentry.dev/cb", UriType.LOGIN));
        system.getRedirectUris().add(new RedirectUri("https://old.sentry.dev", UriType.LOGOUT));
        when(systemRepository.findById(1L)).thenReturn(Optional.of(system));

        systemService.update(1L, updateRequest(null, List.of("https://new.sentry.dev/cb")));

        assertThat(captureSaved().getRedirectUris()).containsExactlyInAnyOrder(
                new RedirectUri("https://new.sentry.dev/cb", UriType.LOGIN),
                new RedirectUri("https://old.sentry.dev", UriType.LOGOUT));
    }

    private ExternalSystem captureSaved() {
        ArgumentCaptor<ExternalSystem> saved = ArgumentCaptor.forClass(ExternalSystem.class);
        verify(systemRepository).save(saved.capture());
        return saved.getValue();
    }
}
