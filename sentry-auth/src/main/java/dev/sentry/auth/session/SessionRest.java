package dev.sentry.auth.session;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;

/** Contratos da API de sessão SSO, consumida pela tela de login do front-end (React). */
public interface SessionRest {

    @Schema(description = "Contexto da tela de login: o sistema que pediu a autenticação, quando há um fluxo em andamento")
    record Context(
            @Schema(description = "client_id do sistema", example = "SYS001") String clientId,
            @Schema(description = "Nome do sistema para a tela exibir", example = "Sistema de Exemplo") String systemName) {
    }

    @Schema(description = "Credenciais digitadas na tela de login")
    record LoginRequest(
            @Schema(description = "Nome de usuário", example = "maria", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String username,
            @Schema(description = "Senha", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String password) {

        // a senha não pode aparecer em logs (RN-F0-02.6)
        @Override
        public String toString() {
            return "LoginRequest[username=" + username + ", password=***]";
        }
    }

    @Schema(description = "Resultado do login")
    record LoginResponse(
            @Schema(description = "URL para o navegador abrir e retomar a autorização; nula sem fluxo em andamento")
            String redirectUrl) {
    }

    @Schema(description = "Usuário da sessão SSO")
    record Me(
            @Schema(description = "Indica se há sessão autenticada", example = "true") boolean authenticated,
            @Schema(description = "Identificador público do usuário") String sub,
            @Schema(description = "Nome de usuário", example = "maria") String username,
            @Schema(description = "Nome", example = "Maria Silva") String name) {

        static final Me ANONYMOUS = new Me(false, null, null, null);
    }
}
