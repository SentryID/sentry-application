package dev.sentry.api.domain.passwordtoken;

import dev.sentry.api.domain.shared.AuditableEntity;
import dev.sentry.api.domain.shared.SecureToken;
import dev.sentry.api.domain.shared.TokenValidity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Token de redefinição de senha. Uso único, com valor sempre gerado pelo servidor. */
@Getter
@Entity
@Table(name = "password_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordToken extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_password_token")
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "id_user", nullable = false)
    private Long idUser;

    @Embedded
    private TokenValidity validity;

    public static PasswordToken issue(Long idUser, LocalDateTime validUntil) {
        PasswordToken passwordToken = new PasswordToken();
        passwordToken.idUser = idUser;
        passwordToken.token = SecureToken.generate();
        passwordToken.validity = TokenValidity.validUntil(validUntil);
        return passwordToken;
    }

    /** Uso único: recusa se já foi consumido ou se expirou. */
    public void consume(LocalDateTime agora) {
        this.validity = validity.consume(agora);
    }
}
