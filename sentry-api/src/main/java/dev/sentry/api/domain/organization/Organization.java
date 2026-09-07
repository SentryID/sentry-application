package dev.sentry.api.domain.organization;

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
@Table(name = "organizations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "external_code", unique = true)
    private String externalCode;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    public static Organization create(String name, String externalCode, Boolean isActive) {
        Organization organization = new Organization();
        organization.name = name;
        organization.externalCode = externalCode;
        organization.changeActivation(isActive);
        return organization;
    }

    public void update(String name, String externalCode, Boolean isActive) {
        this.name = name;
        this.externalCode = externalCode;
        changeActivation(isActive);
    }
}
