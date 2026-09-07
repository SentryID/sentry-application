package dev.sentry.api.domain.role;

import dev.sentry.api.domain.action.Action;
import dev.sentry.api.domain.shared.SoftDeletableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Raiz do agregado perfil de acesso. As opções ({@link RoleOption}) só entram e saem por
 * {@link #grant(Action)} e {@link #revoke(Long)} — é aí que mora a regra de que uma ação
 * concedida tem de pertencer ao mesmo sistema do perfil.
 */
@Getter
@Entity
@Table(name = "roles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private Long id;

    @Column(name = "id_system", nullable = false)
    private Long idSystem;

    @Column(name = "id_organization", nullable = false)
    private Long idOrganization;

    @Column(name = "system_role")
    private String systemRole;

    @Column(name = "external_role")
    private String externalRole;

    @Column(name = "role_type")
    private String roleType;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RoleOption> options = new ArrayList<>();

    public static Role create(Long idSystem, Long idOrganization, String systemRole, String externalRole,
            String roleType, Boolean isActive) {
        Role role = new Role();
        role.idSystem = idSystem;
        role.idOrganization = idOrganization;
        role.systemRole = systemRole;
        role.externalRole = externalRole;
        role.roleType = roleType;
        role.changeActivation(isActive);
        return role;
    }

    public void update(Long idSystem, Long idOrganization, String systemRole, String externalRole, String roleType,
            Boolean isActive) {
        this.idSystem = idSystem;
        this.idOrganization = idOrganization;
        this.systemRole = systemRole;
        this.externalRole = externalRole;
        this.roleType = roleType;
        changeActivation(isActive);
    }

    /** Concede uma ação ao perfil. Conceder de novo o que já está concedido é no-op. */
    public void grant(Action action) {
        if (!Objects.equals(action.getIdSystem(), this.idSystem)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A ação pertence a outro sistema e não pode ser concedida a esse perfil");
        }
        options.stream()
                .filter(option -> Objects.equals(option.getCdAction(), action.getId()))
                .findFirst()
                .ifPresentOrElse(RoleOption::restore, () -> options.add(RoleOption.of(this, action.getId())));
    }

    public void revoke(Long cdAction) {
        options.stream()
                .filter(option -> Objects.equals(option.getCdAction(), cdAction) && !option.getIsDeleted())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Essa ação não está vinculada ao perfil"))
                .delete();
    }

    public boolean hasAction(Long cdAction) {
        return options.stream()
                .anyMatch(option -> Objects.equals(option.getCdAction(), cdAction) && !option.getIsDeleted());
    }

    /** Só as opções vigentes; as revogadas continuam na tabela por exclusão lógica. */
    public List<RoleOption> getActiveOptions() {
        return options.stream().filter(option -> !option.getIsDeleted()).toList();
    }

    /** Cópia imutável — a coleção só muda por {@link #grant} e {@link #revoke}. */
    public List<RoleOption> getOptions() {
        return List.copyOf(options);
    }
}
