package dev.sentry.auth.bootstrap;

import dev.sentry.api.domain.access.role.Role;
import dev.sentry.api.domain.access.role.api.RoleRepository;
import dev.sentry.api.domain.access.roleuser.RoleUser;
import dev.sentry.api.domain.access.roleuser.api.RoleUserRepository;
import dev.sentry.api.domain.organization.Organization;
import dev.sentry.api.domain.organization.api.OrganizationRepository;
import dev.sentry.api.domain.system.ExternalSystem;
import dev.sentry.api.domain.system.api.SystemRepository;
import dev.sentry.api.domain.user.User;
import dev.sentry.api.domain.user.api.UserRepository;
import dev.sentry.api.security.password.PasswordService;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria, na primeira subida com banco vazio, a organização da plataforma, o sistema `SENTRY` e o
 * administrador (RN-F0-03.3 a RN-F0-03.5).
 */
@Service
@EnableConfigurationProperties(BootstrapProperties.class)
public class BootstrapService implements ApplicationRunner {

    public static final String PLATFORM_ORGANIZATION = "SENTRY_PLATFORM";
    public static final String PLATFORM_SYSTEM = "SENTRY";
    public static final String ADMIN_ROLE = "SENTRY.ADMIN";
    static final int GENERATED_PASSWORD_LENGTH = 24;

    private static final String AUDIT_USER = "bootstrap";
    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*-_=+";
    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);

    private final BootstrapProperties properties;
    private final OrganizationRepository organizationRepository;
    private final SystemRepository systemRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RoleUserRepository roleUserRepository;
    private final PasswordService passwordService;
    private final SecureRandom random = new SecureRandom();

    public BootstrapService(BootstrapProperties properties, OrganizationRepository organizationRepository,
            SystemRepository systemRepository, RoleRepository roleRepository, UserRepository userRepository,
            RoleUserRepository roleUserRepository, PasswordService passwordService) {
        this.properties = properties;
        this.organizationRepository = organizationRepository;
        this.systemRepository = systemRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.roleUserRepository = roleUserRepository;
        this.passwordService = passwordService;
    }

    /**
     * Na subida do serviço, cria a organização da plataforma, o sistema `SENTRY`, o papel `SENTRY.ADMIN` e o
     * administrador, com senha em hash. Se a organização da plataforma já existe, não faz nada (RN-F0-03.5).
     * Sem senha configurada, gera uma e a mostra uma única vez no log (RN-F0-03.4).
     *
     * @param args argumentos da aplicação (não usados)
     * @throws IllegalStateException se já existe um usuário com o login ou o e-mail do administrador
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (organizationRepository.findByExternalCode(PLATFORM_ORGANIZATION) != null) {
            return;
        }
        if (userRepository.findByUsernameIgnoreCase(properties.username()) != null
                || userRepository.findByEmail(properties.email()) != null) {
            throw new IllegalStateException(
                    "Já existe um usuário com o login ou o e-mail do administrador; ajuste SENTRY_ADMIN_USERNAME e SENTRY_ADMIN_EMAIL");
        }

        Organization organization = new Organization();
        organization.setName("Sentry");
        organization.setExternalCode(PLATFORM_ORGANIZATION);
        organization.setCreatedBy(AUDIT_USER);
        organization.setUpdatedBy(AUDIT_USER);
        organizationRepository.save(organization);

        ExternalSystem system = new ExternalSystem();
        system.setOrganization(organization);
        system.setExternalCode(PLATFORM_SYSTEM);
        system.setDescription("Sentry");
        system.setScopes("openid profile email");
        system.setCreatedBy(AUDIT_USER);
        system.setUpdatedBy(AUDIT_USER);
        systemRepository.save(system);

        Role role = new Role();
        role.setSystem(system);
        role.setOrganization(organization);
        role.setSystemRole(ADMIN_ROLE);
        role.setRoleType("ACCESS");
        role.setCreatedBy(AUDIT_USER);
        role.setUpdatedBy(AUDIT_USER);
        roleRepository.save(role);

        boolean generated = properties.password() == null || properties.password().isBlank();
        String password = generated ? generatePassword() : properties.password();

        User admin = new User();
        admin.setName("Administrador");
        admin.setAuthenticationType("DATABASE");
        admin.setUsername(properties.username());
        admin.setEmail(properties.email());
        admin.setPassword(passwordService.hash(password));
        admin.setCreatedBy(AUDIT_USER);
        admin.setUpdatedBy(AUDIT_USER);
        userRepository.save(admin);

        RoleUser roleUser = new RoleUser();
        roleUser.setRole(role);
        roleUser.setUser(admin);
        roleUser.setCreatedBy(AUDIT_USER);
        roleUser.setUpdatedBy(AUDIT_USER);
        roleUserRepository.save(roleUser);

        if (generated) {
            // única vez em que a senha aparece; a troca obrigatória no primeiro login entra na Fase 2
            log.warn("Administrador '{}' criado com a senha gerada: {} (anote agora; ela não será mostrada de novo)",
                    properties.username(), password);
        } else {
            log.info("Administrador '{}' criado com a senha de SENTRY_ADMIN_PASSWORD", properties.username());
        }
    }

    /**
     * Gera uma senha aleatória para o administrador.
     *
     * @return senha com {@value #GENERATED_PASSWORD_LENGTH} caracteres
     */
    String generatePassword() {
        StringBuilder password = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }
}
