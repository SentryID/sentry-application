package dev.sentry.api.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/** Auditoria mais os dois sinalizadores de ativação e exclusão lógica. */
@Getter
@MappedSuperclass
public abstract class SoftDeletableEntity extends AuditableEntity {

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /** Liga ou desliga o registro; nulo não altera nada. */
    public void changeActivation(Boolean active) {
        if (active != null) {
            this.isActive = active;
        }
    }

    /** Exclusão lógica: o registro some das listagens, mas continua na tabela. */
    public void delete() {
        this.isDeleted = true;
    }

    /** Desfaz a exclusão lógica — reaproveita a linha em vez de duplicar. */
    public void restore() {
        this.isDeleted = false;
        this.isActive = true;
    }
}
