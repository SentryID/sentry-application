package dev.sentry.auth.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sentry.api.domain.access.role.api.RoleRepository;
import dev.sentry.api.domain.access.roleuser.api.RoleUserRepository;
import dev.sentry.api.domain.organization.Organization;
import dev.sentry.api.domain.organization.api.OrganizationRepository;
import dev.sentry.api.domain.system.api.SystemRepository;
import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.api.UserRepository;
import dev.sentry.api.security.password.PasswordService;
import org.junit.jupiter.api.Test;

class BootstrapServiceTest {

    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final BootstrapService bootstrapService = new BootstrapService(
            new BootstrapProperties("admin", "admin@sentry.local", null), organizationRepository,
            mock(SystemRepository.class), mock(RoleRepository.class), userRepository,
            mock(RoleUserRepository.class), mock(PasswordService.class));

    // RN-F0-03.5
    @Test
    void doesNothingWhenPlatformAlreadyExists() {
        when(organizationRepository.findByExternalCode(BootstrapService.PLATFORM_ORGANIZATION))
                .thenReturn(new Organization());

        assertFalse(bootstrapService.bootstrap());

        verify(organizationRepository, never()).save(any());
    }

    @Test
    void failsWhenAdminUsernameIsTaken() {
        when(userRepository.findByUsername("admin")).thenReturn(new User());

        assertThrows(IllegalStateException.class, bootstrapService::bootstrap);
        verify(organizationRepository, never()).save(any());
    }

    // RN-F0-03.4
    @Test
    void generatedPasswordHasAtLeastTwentyCharactersAndIsRandom() {
        String password = bootstrapService.generatePassword();

        assertEquals(BootstrapService.GENERATED_PASSWORD_LENGTH, password.length());
        assertFalse(password.equals(bootstrapService.generatePassword()));
    }

    @Test
    void propertiesDoNotExposePasswordInToString() {
        assertFalse(new BootstrapProperties("admin", "a@b.c", "S3nha!forte").toString().contains("S3nha!forte"));
    }
}
