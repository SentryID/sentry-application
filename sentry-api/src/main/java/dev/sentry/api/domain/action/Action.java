package dev.sentry.api.domain.action;

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

@Getter
@Entity
@Table(name = "actions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Action extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cd_action")
    private Long id;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "id_system", nullable = false)
    private Long idSystem;

    public static Action create(String actionType, Long idSystem, Boolean isActive) {
        Action action = new Action();
        action.actionType = actionType;
        action.idSystem = idSystem;
        action.changeActivation(isActive);
        return action;
    }

    public void update(String actionType, Long idSystem, Boolean isActive) {
        this.actionType = actionType;
        this.idSystem = idSystem;
        changeActivation(isActive);
    }
}
