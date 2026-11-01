package dev.sentry.auth.provider.ldap;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Servidor LDAP que confere a senha dos usuários {@code LDAP} (RN-F1-02.7). Sem {@code url}, o LDAP fica desligado
 * e esses usuários recebem {@code INVALID_CREDENTIALS}.
 *
 * <p>O DN do usuário é achado de um de dois jeitos. Com {@code userSearchFilter} preenchido, uma conta de serviço
 * ({@code managerDn}) busca o usuário e o bind é feito com o DN encontrado; é o caso do Active Directory, com usuários
 * em várias OUs. Sem ele, o DN é montado pelo {@code userDnPattern}.
 *
 * @param url endereço do servidor, ex.: {@code ldaps://ldap.exemplo.com:636}
 * @param base DN base, ex.: {@code dc=exemplo,dc=com}
 * @param userDnPattern DN do usuário relativo à base; {@code {0}} é o username. Ignorado quando há busca
 * @param userSearchBase onde buscar o usuário, relativo à base, ex.: {@code OU=Usuarios}; vazio busca na base toda
 * @param userSearchFilter filtro da busca; {@code {0}} é o username, ex.: {@code (sAMAccountName={0})}
 * @param managerDn conta de serviço que faz a busca, ex.: {@code svc-sentry@exemplo.com}
 * @param managerPassword senha da conta de serviço
 * @param connectTimeout tempo máximo para abrir a conexão com o servidor
 * @param readTimeout tempo máximo de espera pela resposta do servidor
 */
@ConfigurationProperties("sentry.ldap")
public record LdapProperties(
        String url,
        String base,
        @DefaultValue("uid={0},ou=people") String userDnPattern,
        @DefaultValue("") String userSearchBase,
        String userSearchFilter,
        String managerDn,
        String managerPassword,
        @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout) {

    /** @return se o DN do usuário é achado por busca, e não montado pelo {@code userDnPattern} */
    public boolean usesSearch() {
        return userSearchFilter != null && !userSearchFilter.isBlank();
    }

    // a senha da conta de serviço não pode aparecer em logs (RN-F0-02.6)
    @Override
    public String toString() {
        return "LdapProperties[url=" + url + ", base=" + base + ", userDnPattern=" + userDnPattern
                + ", userSearchBase=" + userSearchBase + ", userSearchFilter=" + userSearchFilter
                + ", managerDn=" + managerDn + ", managerPassword=***, connectTimeout=" + connectTimeout
                + ", readTimeout=" + readTimeout + "]";
    }
}
