package dev.sentry.api.security.password;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Geração e verificação de hash de senha. Nenhum método registra ou devolve a senha em claro (RN-F0-02.6). */
@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    public PasswordService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Gera o hash da senha com o algoritmo e os parâmetros configurados.
     *
     * @param rawPassword senha em claro
     * @return hash no formato `{argon2}$argon2id$v=19$m=...,t=...,p=...$salt$hash`
     */
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verifica a senha contra o hash armazenado. Se a senha confere e o hash usa parâmetros mais fracos
     * que os configurados (ou outro algoritmo), devolve também o novo hash para ser gravado (RN-F0-02.4).
     *
     * @param rawPassword senha em claro informada pelo usuário
     * @param storedHash hash gravado; `null` nunca confere
     * @return resultado da verificação e, quando for o caso, o novo hash
     */
    public Verification verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || !passwordEncoder.matches(rawPassword, storedHash)) {
            return new Verification(false, null);
        }
        String upgradedHash = passwordEncoder.upgradeEncoding(storedHash) ? passwordEncoder.encode(rawPassword) : null;
        return new Verification(true, upgradedHash);
    }

    /**
     * Resultado de {@link #verify(String, String)}.
     *
     * @param matches indica se a senha confere
     * @param upgradedHash novo hash a gravar, ou `null` quando o atual já está atualizado
     */
    public record Verification(boolean matches, String upgradedHash) {
    }
}
