package dev.sentry.api.domain.user;

import dev.sentry.api.domain.shared.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Embedded
    private Username username;

    @Embedded
    private Email email;

    @Embedded
    private Credentials credentials;

    /** A senha entra em claro e sai derivada: não existe caminho que grave texto puro. */
    public static User register(String name, Username username, Email email, String authenticationType,
            String senhaEmClaro, Boolean isActive) {
        User user = new User();
        user.name = name;
        user.username = username;
        user.email = email;
        user.credentials = Credentials.hash(senhaEmClaro, authenticationType);
        user.changeActivation(isActive);
        return user;
    }

    /** O login não muda depois do cadastro, e a senha tem caminho próprio. */
    public void update(String name, Email email, String authenticationType, Boolean isActive) {
        this.name = name;
        this.email = email;
        this.credentials = credentials.withAuthenticationType(authenticationType);
        changeActivation(isActive);
    }

    public void changePassword(String senhaEmClaro) {
        this.credentials = Credentials.hash(senhaEmClaro, credentials.authenticationType());
    }

    public boolean matchesPassword(String senhaEmClaro) {
        return credentials != null && credentials.matches(senhaEmClaro);
    }
}
