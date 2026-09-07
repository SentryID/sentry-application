package dev.sentry.api.domain.role;

import dev.sentry.api.domain.shared.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entidade interna do agregado {@link Role} — não é raiz. A fábrica é
 * package-private de propósito: só o próprio {@code Role} cria uma opção, e sempre
 * depois de checar que a ação pertence ao mesmo sistema do perfil.
 */
@Getter
@Entity
@Table(name = "role_options")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleOption extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role_option")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_role", nullable = false)
    private Role role;

    @Column(name = "cd_action", nullable = false)
    private Long cdAction;

    static RoleOption of(Role role, Long cdAction) {
        RoleOption option = new RoleOption();
        option.role = role;
        option.cdAction = cdAction;
        return option;
    }
}
