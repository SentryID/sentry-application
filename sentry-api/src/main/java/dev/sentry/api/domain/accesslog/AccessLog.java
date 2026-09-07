package dev.sentry.api.domain.accesslog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Registro de tentativa de acesso. É trilha de auditoria: só nasce e é lido, nunca é
 * alterado nem apagado — por isso não há {@code update} nem {@code delete}.
 */
@Getter
@Entity
@Table(name = "access_logs")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_access_log")
    private Long id;

    @Column(name = "id_user")
    private Long idUser;

    @CreatedDate
    @Column(name = "attempted_at", updatable = false)
    private LocalDateTime attemptedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_result", nullable = false)
    private AttemptResult attemptResult;

    @Column(name = "ip_host")
    private String ipHost;

    @Column(name = "id_system")
    private Long idSystem;

    public static AccessLog record(Long idUser, Long idSystem, AttemptResult attemptResult, String ipHost) {
        AccessLog accessLog = new AccessLog();
        accessLog.idUser = idUser;
        accessLog.idSystem = idSystem;
        accessLog.attemptResult = attemptResult;
        accessLog.ipHost = ipHost;
        return accessLog;
    }
}
