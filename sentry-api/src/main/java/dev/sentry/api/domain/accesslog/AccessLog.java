package dev.sentry.api.domain.accesslog;

import dev.sentry.api.domain.system.System;
import dev.sentry.api.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user")
    private User user;

    @CreatedDate
    @Column(name = "attempted_at", updatable = false)
    private LocalDateTime attemptedAt;

    @Column(name = "attempt_result")
    private String attemptResult;

    @Column(name = "ip_host")
    private String ipHost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_system")
    private System system;
}
