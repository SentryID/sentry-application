package dev.sentry.api.domain.roleuser;

import dev.sentry.api.domain.shared.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Vínculo entre um grupo de perfis e um usuário.
 *
 * <p>Continua sendo uma associação simples, sem comportamento próprio: a invariante
 * natural — usuário e perfil na mesma organização — depende da tabela {@code groups},
 * que ainda não foi modelada.
 */
@Getter
@Entity
@Table(name = "role_users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleUser extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role_user")
    private Long id;

    @Column(name = "id_group", nullable = false)
    private Long idGroup;

    @Column(name = "id_user", nullable = false)
    private Long idUser;

    public static RoleUser assign(Long idGroup, Long idUser, Boolean isActive) {
        RoleUser roleUser = new RoleUser();
        roleUser.idGroup = idGroup;
        roleUser.idUser = idUser;
        roleUser.changeActivation(isActive);
        return roleUser;
    }

    public void update(Long idGroup, Long idUser, Boolean isActive) {
        this.idGroup = idGroup;
        this.idUser = idUser;
        changeActivation(isActive);
    }
}
