package dev.sentry.auth.oauth;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.sentry.auth.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthorizationServerMetadataTest {

    private static final String ISSUER = "http://localhost:8081";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // CA-F1-07.1, RN-F1-07.1, RN-F1-07.6
    @Test
    void discoveryPublishesIssuerEndpointsAndPkceOnly() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=300")))
                .andExpect(jsonPath("$.issuer").value(ISSUER))
                .andExpect(jsonPath("$.authorization_endpoint").value(ISSUER + "/oauth2/authorize"))
                .andExpect(jsonPath("$.token_endpoint").value(ISSUER + "/oauth2/token"))
                .andExpect(jsonPath("$.userinfo_endpoint").value(ISSUER + "/userinfo"))
                .andExpect(jsonPath("$.jwks_uri").value(ISSUER + "/oauth2/jwks"))
                .andExpect(jsonPath("$.revocation_endpoint").value(ISSUER + "/oauth2/revoke"))
                .andExpect(jsonPath("$.end_session_endpoint").value(ISSUER + "/connect/logout"))
                .andExpect(jsonPath("$.response_types_supported", contains("code")))
                .andExpect(jsonPath("$.code_challenge_methods_supported", contains("S256")))
                .andExpect(jsonPath("$.grant_types_supported", containsInAnyOrder("authorization_code", "refresh_token")))
                .andExpect(jsonPath("$.id_token_signing_alg_values_supported", contains("RS256")))
                .andExpect(jsonPath("$.scopes_supported", contains("openid")));
    }

    // RN-F1-07.2, RN-F1-07.6
    @Test
    void jwksPublishesPublicKeysWithKid() throws Exception {
        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=300")))
                .andExpect(jsonPath("$.keys[0].kid").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
    }

    // CA-F1-01.3, RN-F1-03.3: cliente desativado não recebe redirecionamento
    @Test
    void inactiveSystemCannotStartAuthorization() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO organizations (name, external_code, uuid, is_active, is_deleted)
                VALUES ('Org Inativa', 'ORG_OFF', gen_random_uuid(), TRUE, FALSE)""");
        jdbcTemplate.update("""
                INSERT INTO systems (id_organization, external_code, client_type, uuid, is_active, is_deleted)
                SELECT id_organization, 'SYS_OFF', 'PUBLIC', gen_random_uuid(), FALSE, FALSE
                FROM organizations WHERE external_code = 'ORG_OFF'""");
        jdbcTemplate.update("""
                INSERT INTO system_redirect_uris (id_system, uri, uri_type)
                SELECT id_system, 'http://localhost:3000/cb', 'LOGIN' FROM systems WHERE external_code = 'SYS_OFF'""");

        mockMvc.perform(get("/oauth2/authorize")
                        .param("response_type", "code").param("client_id", "SYS_OFF")
                        .param("redirect_uri", "http://localhost:3000/cb").param("scope", "openid")
                        .param("code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                        .param("code_challenge_method", "S256"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Location"));
    }
}
