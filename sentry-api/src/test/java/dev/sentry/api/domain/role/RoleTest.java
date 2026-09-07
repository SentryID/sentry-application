package dev.sentry.api.domain.role;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.sentry.api.domain.action.Action;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RoleTest {

    private static final Long SISTEMA = 1L;
    private static final Long OUTRO_SISTEMA = 2L;

    @Test
    void concedeAcaoDoMesmoSistema() {
        Role role = perfilDoSistema(SISTEMA);

        role.grant(acao(10L, SISTEMA));

        assertTrue(role.hasAction(10L));
        assertEquals(1, role.getActiveOptions().size());
    }

    @Test
    void recusaAcaoDeOutroSistema() {
        Role role = perfilDoSistema(SISTEMA);

        ResponseStatusException erro =
                assertThrows(ResponseStatusException.class, () -> role.grant(acao(10L, OUTRO_SISTEMA)));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertFalse(role.hasAction(10L));
    }

    @Test
    void concederDuasVezesNaoDuplica() {
        Role role = perfilDoSistema(SISTEMA);
        Action action = acao(10L, SISTEMA);

        role.grant(action);
        role.grant(action);

        assertEquals(1, role.getActiveOptions().size());
    }

    @Test
    void revogarTiraDaListaVigenteMasMantemALinha() {
        Role role = perfilDoSistema(SISTEMA);
        role.grant(acao(10L, SISTEMA));

        role.revoke(10L);

        assertFalse(role.hasAction(10L));
        assertEquals(0, role.getActiveOptions().size());
        assertEquals(1, role.getOptions().size());
    }

    @Test
    void concederDeNovoReaproveitaALinhaRevogada() {
        Role role = perfilDoSistema(SISTEMA);
        role.grant(acao(10L, SISTEMA));
        role.revoke(10L);

        role.grant(acao(10L, SISTEMA));

        assertTrue(role.hasAction(10L));
        assertEquals(1, role.getOptions().size());
    }

    @Test
    void revogarOQueNaoFoiConcedidoFalha() {
        Role role = perfilDoSistema(SISTEMA);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> role.revoke(99L));

        assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
    }

    private static Role perfilDoSistema(Long idSystem) {
        return Role.create(idSystem, 1L, "ADMIN", "ADMINISTRADOR", "INTERNAL", true);
    }

    /** O id da ação é gerado pelo banco, então aqui é preenchido por reflexão. */
    private static Action acao(Long cdAction, Long idSystem) {
        Action action = Action.create("READ", idSystem, true);
        try {
            Field id = Action.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(action, cdAction);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return action;
    }
}
