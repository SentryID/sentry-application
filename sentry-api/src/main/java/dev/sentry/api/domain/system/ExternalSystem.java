package dev.sentry.api.domain.system;

import dev.sentry.api.domain.organization.Organization;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(name = "systems")
@EntityListeners(AuditingEntityListener.class)
public class ExternalSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_system")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organization", nullable = false)
    private Organization organization;

    /** Também é o {@code client_id} OAuth do sistema (RN-F1-01.1). */
    @Column(name = "external_code", nullable = false, unique = true)
    private String externalCode;

    @Column(name = "description")
    private String description;

    @Column(name = "url")
    private String url;

    /** Escopos permitidos, separados por espaço (RN-F1-01.6). */
    @Column(name = "scopes")
    private String scopes;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false)
    private ClientType clientType = ClientType.CONFIDENTIAL;

    @Column(name = "client_secret_hash")
    private String clientSecretHash;

    @Column(name = "is_trusted", nullable = false)
    private Boolean isTrusted = false;

    @ElementCollection
    @CollectionTable(name = "system_redirect_uris", joinColumns = @JoinColumn(name = "id_system"))
    private Set<RedirectUri> redirectUris = new HashSet<>();

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ClientType { PUBLIC, CONFIDENTIAL }

    public enum UriType { LOGIN, LOGOUT }

    /** URI de redirecionamento, comparada por igualdade exata (RN-F1-01.4). */
    @Embeddable
    public record RedirectUri(
            @Column(name = "uri", nullable = false, length = 2048) String uri,
            @Enumerated(EnumType.STRING) @Column(name = "uri_type", nullable = false) UriType type) {
    }
}
