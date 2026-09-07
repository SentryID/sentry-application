package dev.sentry.api.domain.system;

import dev.sentry.api.domain.shared.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "systems")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class System extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_system")
    private Long id;

    @Column(name = "id_organization", nullable = false)
    private Long idOrganization;

    @Column(name = "external_code", unique = true)
    private String externalCode;

    private String description;

    private String url;

    private String scopes;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    public static System create(Long idOrganization, String externalCode, String description, String url,
            String scopes, Boolean isActive) {
        System system = new System();
        system.idOrganization = idOrganization;
        system.externalCode = externalCode;
        system.description = description;
        system.url = url;
        system.scopes = scopes;
        system.changeActivation(isActive);
        return system;
    }

    public void update(Long idOrganization, String externalCode, String description, String url, String scopes,
            Boolean isActive) {
        this.idOrganization = idOrganization;
        this.externalCode = externalCode;
        this.description = description;
        this.url = url;
        this.scopes = scopes;
        changeActivation(isActive);
    }
}
