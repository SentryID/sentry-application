package dev.sentry.api.domain.accesslog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(name = "access_logs")
@EntityListeners(AuditingEntityListener.class)
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

    @Column(name = "attempt_result")
    private String attemptResult;

    @Column(name = "ip_host")
    private String ipHost;

    @Column(name = "id_system")
    private Long idSystem;
}
