package dev.sentry.auth.login;

import org.springframework.stereotype.Service;

@Service
public class LoginService {

    /**
     * Autentica o usuário no sistema informado e emite os tokens de acesso.
     *
     * @param request credenciais do usuário e sistema de destino
     * @return dados do usuário autenticado com os tokens de usuário e de aplicação
     * @throws UnsupportedOperationException enquanto o login não for implementado
     */
    public LoginRest.LoginResponse login(LoginRest.LoginRequest request) {
        throw new UnsupportedOperationException("Login ainda não implementado");
    }
}
