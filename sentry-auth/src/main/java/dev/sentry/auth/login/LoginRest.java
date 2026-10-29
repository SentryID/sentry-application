package dev.sentry.auth.login;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;

/** Contratos do login direto (F1-11). */
public interface LoginRest {

    @Schema(description = "Credenciais do usuário; o sistema chamador se autentica por HTTP Basic")
    record LoginRequest(
            @Schema(description = "Nome de usuário", example = "john.doe", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String username,
            @Schema(description = "Senha do usuário", requiredMode = RequiredMode.REQUIRED)
            @NotBlank String password,
            @Schema(description = "Escopos pedidos, separados por espaço; padrão openid profile", example = "openid profile offline_access")
            String scope) {

        // a senha não pode aparecer em logs (RN-F0-02.6)
        @Override
        public String toString() {
            return "LoginRequest[username=" + username + ", password=***, scope=" + scope + "]";
        }
    }

    @Schema(description = "Resposta de token do OAuth 2.0 (RFC 6749, 5.1)")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TokenResponse(
            @Schema(description = "Access token JWT") @JsonProperty("access_token") String accessToken,
            @Schema(description = "Sempre Bearer", example = "Bearer") @JsonProperty("token_type") String tokenType,
            @Schema(description = "Segundos até o access token expirar", example = "900") @JsonProperty("expires_in") long expiresIn,
            @Schema(description = "ID token do OpenID Connect, com o escopo openid") @JsonProperty("id_token") String idToken,
            @Schema(description = "Refresh token, com o escopo offline_access") @JsonProperty("refresh_token") String refreshToken,
            @Schema(description = "Escopos concedidos", example = "openid profile") String scope) {

        // tokens não podem aparecer em logs (RN-F1-10.7)
        @Override
        public String toString() {
            return "TokenResponse[tokenType=" + tokenType + ", expiresIn=" + expiresIn + ", scope=" + scope + "]";
        }
    }

    @Schema(description = "Erro no formato do OAuth 2.0")
    record ErrorResponse(
            @Schema(example = "invalid_grant") String error,
            @Schema(example = "Usuário ou senha inválidos") @JsonProperty("error_description") String errorDescription) {
    }
}
