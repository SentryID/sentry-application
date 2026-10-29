package dev.sentry.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/** Percorre o fluxo Authorization Code + PKCE como um navegador e um sistema externo fariam (F1-03). */
public final class PkceFlow {

    private PkceFlow() {
    }

    /**
     * Abre a autorização, faz login na tela do Sentry e volta à autorização.
     *
     * @return a sessão SSO criada e o {@code Location} final (o {@code redirect_uri} com {@code code} ou {@code error})
     */
    public static Result loginAndAuthorize(MockMvc mockMvc, String authorizeUrl, String username, String password)
            throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(get(URI.create(authorizeUrl)).accept("text/html"))
                .andReturn().getRequest().getSession();
        String resume = mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", username).param("password", password))
                .andReturn().getResponse().getRedirectedUrl();
        String location = mockMvc.perform(get(URI.create(resume)).session(session)).andReturn().getResponse().getRedirectedUrl();
        return new Result(session, location);
    }

    /** Pede a autorização com uma sessão SSO já existente (sem passar pela tela). */
    public static String authorize(MockMvc mockMvc, MockHttpSession session, String authorizeUrl) throws Exception {
        return mockMvc.perform(get(URI.create(authorizeUrl)).session(session).accept("text/html"))
                .andReturn().getResponse().getRedirectedUrl();
    }

    /** Pede a autorização sem sessão e devolve o {@code Location} da resposta (erros voltam antes da tela de login). */
    public static String authorizeAnonymously(MockMvc mockMvc, String authorizeUrl) throws Exception {
        return mockMvc.perform(get(URI.create(authorizeUrl)).accept("text/html")).andReturn().getResponse().getRedirectedUrl();
    }

    /** Valor de um parâmetro da query string de uma URL. */
    public static String param(String url, String name) {
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst(name);
    }

    /**
     * Troca o código por tokens. Com {@code clientSecret} nulo, o cliente é público e manda só o {@code client_id}.
     */
    public static ResultActions exchange(MockMvc mockMvc, String clientId, String clientSecret, String code,
            String codeVerifier) throws Exception {
        MockHttpServletRequestBuilder request = post("/oauth2/token")
                .param("grant_type", "authorization_code").param("code", code)
                .param("redirect_uri", OAuthFixtures.REDIRECT_URI).param("code_verifier", codeVerifier);
        return mockMvc.perform(withClient(request, clientId, clientSecret));
    }

    /** Usa um refresh token no {@code /oauth2/token}. */
    public static ResultActions refresh(MockMvc mockMvc, String clientId, String clientSecret, String refreshToken)
            throws Exception {
        return mockMvc.perform(withClient(post("/oauth2/token")
                .param("grant_type", "refresh_token").param("refresh_token", refreshToken), clientId, clientSecret));
    }

    public static MockHttpServletRequestBuilder withClient(MockHttpServletRequestBuilder request, String clientId,
            String clientSecret) {
        request.contentType(MediaType.APPLICATION_FORM_URLENCODED);
        return clientSecret == null ? request.param("client_id", clientId) : request.with(httpBasic(clientId, clientSecret));
    }

    /** Lê um campo de uma resposta JSON. */
    public static String json(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    public record Result(MockHttpSession session, String location) {

        public String code() {
            return param(location, "code");
        }
    }
}
