package dev.sentry.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/** Percorre o fluxo Authorization Code + PKCE como um navegador e um sistema externo fariam (F1-03). */
public final class PkceFlow {

    public static final String SESSION = "SESSION";

    private PkceFlow() {
    }

    /**
     * Abre a autorização, faz login pela API de sessão (como a tela do front-end faz) e retoma a autorização pela URL
     * devolvida. A sessão SSO (Spring Session) segue no cookie {@code SESSION}, como num navegador.
     *
     * @return o cookie da sessão SSO e o {@code Location} final (o {@code redirect_uri} com {@code code} ou {@code error})
     */
    public static Result loginAndAuthorize(MockMvc mockMvc, String authorizeUrl, String username, String password)
            throws Exception {
        MvcResult start = mockMvc.perform(get(URI.create(authorizeUrl)).accept("text/html")).andReturn();
        Cookie session = start.getResponse().getCookie(SESSION);
        MvcResult login = sessionLogin(mockMvc, session, username, password).andReturn();
        session = sessionAfter(login, session);
        String redirectUrl = JsonPath.read(login.getResponse().getContentAsString(), "$.redirectUrl");
        MvcResult resume = mockMvc.perform(get(URI.create(redirectUrl)).cookie(session)).andReturn();
        return new Result(sessionAfter(resume, session), resume.getResponse().getRedirectedUrl());
    }

    /** {@code POST /api/v1/session/login} com CSRF, como a tela de login do front-end envia. */
    public static ResultActions sessionLogin(MockMvc mockMvc, Cookie session, String username, String password)
            throws Exception {
        var request = post("/api/v1/session/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"" + username + "\", \"password\": \"" + password + "\"}");
        return mockMvc.perform(session == null ? request : request.cookie(session));
    }

    /** Pede a autorização com uma sessão SSO já existente (sem passar pela tela). */
    public static String authorize(MockMvc mockMvc, Cookie session, String authorizeUrl) throws Exception {
        return mockMvc.perform(get(URI.create(authorizeUrl)).cookie(session).accept("text/html"))
                .andReturn().getResponse().getRedirectedUrl();
    }

    /** O cookie de sessão muda no login (proteção contra fixação, RN-F1-09.3). */
    public static Cookie sessionAfter(MvcResult result, Cookie previous) {
        Cookie renewed = result.getResponse().getCookie(SESSION);
        return renewed != null ? renewed : previous;
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

    public record Result(Cookie session, String location) {

        public String code() {
            return param(location, "code");
        }
    }
}
