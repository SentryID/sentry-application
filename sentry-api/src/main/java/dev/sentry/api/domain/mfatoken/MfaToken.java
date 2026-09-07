package dev.sentry.api.domain.mfatoken;

import dev.sentry.api.domain.shared.SecureToken;
import dev.sentry.api.domain.shared.TokenValidity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
 * Desafio de autenticação multifator. Não tem exclusão lógica nem auditoria de alteração
 * — a tabela só guarda quando foi criado e quando foi consumido.
 */
@Getter
@Entity
@Table(name = "mfa_tokens")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MfaToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mfa_token")
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String username;

    @Embedded
    private TokenValidity validity;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** O valor do token é gerado aqui: quem chama escolhe apenas o código enviado ao usuário. */
    public static MfaToken issue(String username, String code, LocalDateTime validUntil) {
        MfaToken mfaToken = new MfaToken();
        mfaToken.username = username;
        mfaToken.code = code;
        mfaToken.token = SecureToken.generate();
        mfaToken.validity = TokenValidity.validUntil(validUntil);
        return mfaToken;
    }

    /** Uso único: recusa se já foi consumido ou se expirou. */
    public void consume(LocalDateTime agora) {
        this.validity = validity.consume(agora);
    }
}
