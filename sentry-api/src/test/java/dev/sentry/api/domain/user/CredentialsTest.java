package dev.sentry.api.domain.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CredentialsTest {

    @Test
    void naoGuardaSenhaEmClaro() {
        Credentials credentials = Credentials.hash("S3nh@Forte", "LOCAL");

        assertNotEquals("S3nh@Forte", credentials.passwordHash());
    }

    @Test
    void reconheceSenhaCertaERecusaErrada() {
        Credentials credentials = Credentials.hash("S3nh@Forte", "LOCAL");

        assertTrue(credentials.matches("S3nh@Forte"));
        assertFalse(credentials.matches("outraSenha"));
        assertFalse(credentials.matches(null));
    }

    @Test
    void mesmaSenhaGeraHashesDiferentes() {
        Credentials primeira = Credentials.hash("S3nh@Forte", "LOCAL");
        Credentials segunda = Credentials.hash("S3nh@Forte", "LOCAL");

        assertNotEquals(primeira.salt(), segunda.salt());
        assertNotEquals(primeira.passwordHash(), segunda.passwordHash());
    }

    @Test
    void trocarTipoDeAutenticacaoPreservaOHash() {
        Credentials original = Credentials.hash("S3nh@Forte", "LOCAL");
        Credentials trocada = original.withAuthenticationType("LDAP");

        assertEquals(original.passwordHash(), trocada.passwordHash());
        assertEquals(original.salt(), trocada.salt());
        assertTrue(trocada.matches("S3nh@Forte"));
    }

    @Test
    void senhaVaziaEhRecusada() {
        assertThrows(IllegalArgumentException.class, () -> Credentials.hash("  ", "LOCAL"));
    }
}
