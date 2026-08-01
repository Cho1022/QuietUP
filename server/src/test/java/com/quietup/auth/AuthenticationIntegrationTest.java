package com.quietup.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("quietup")
            .withUsername("quietup")
            .withPassword("quietup-test");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void logsInWithNormalizedEmailAndIssuesMinimalClaimAccessToken() throws Exception {
        signup();

        MvcResult result = login(" USER@EXAMPLE.COM ", "password123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.accessTokenExpiresIn").value(900))
                .andReturn();

        String accessToken = extractJsonString(result.getResponse().getContentAsString(), "accessToken");
        Jwt jwt = jwtDecoder.decode(accessToken);

        assertEquals(Set.of("sub", "iat", "exp"), jwt.getClaims().keySet());
        assertFalse(jwt.hasClaim("email"));
        assertFalse(jwt.hasClaim("nickname"));
    }

    @Test
    void returnsSameUnauthorizedResponseForUnknownEmailAndWrongPassword() throws Exception {
        signup();

        MvcResult unknownEmail = login("missing@example.com", "password123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();
        MvcResult wrongPassword = login("user@example.com", "wrong-password1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        assertEquals(
                unknownEmail.getResponse().getContentAsString(StandardCharsets.UTF_8),
                wrongPassword.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void returnsCurrentUserForValidAccessToken() throws Exception {
        long userId = signup();
        String accessToken = loginAndGetAccessToken();

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.nickname").value("조용한이웃"));
    }

    @Test
    void rejectsMissingMalformedWrongSignatureAndExpiredAccessTokens() throws Exception {
        long userId = signup();
        String validToken = loginAndGetAccessToken();
        String wrongSignatureToken = encodeWithDifferentKey(userId);
        String expiredToken = encode(jwtEncoder, userId, Instant.now().minusSeconds(180), Instant.now().minusSeconds(120));

        assertInvalidAccessToken(null);
        assertInvalidAccessToken("Bearer");
        assertInvalidAccessToken("Bearer " + wrongSignatureToken);
        assertInvalidAccessToken("Bearer " + expiredToken);
        assertNotEquals(validToken, wrongSignatureToken);
    }

    @Test
    void returnsUserNotFoundWhenJwtSubjectDoesNotExist() throws Exception {
        String token = encode(jwtEncoder, 999_999L, Instant.now(), Instant.now().plusSeconds(900));

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    private long signup() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123",
                                  "nickname": "조용한이웃"
                                }
                                """))
                .andExpect(status().isCreated());

        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = 'user@example.com'", Long.class);
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String loginAndGetAccessToken() throws Exception {
        MvcResult result = login("user@example.com", "password123")
                .andExpect(status().isOk())
                .andReturn();
        return extractJsonString(result.getResponse().getContentAsString(), "accessToken");
    }

    private String encodeWithDifferentKey(long userId) {
        byte[] keyBytes = "fedcba9876543210fedcba9876543210".getBytes(StandardCharsets.UTF_8);
        SecretKey differentKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        JwtEncoder differentEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(differentKey));
        return encode(differentEncoder, userId, Instant.now(), Instant.now().plusSeconds(900));
    }

    private String encode(JwtEncoder encoder, long userId, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(Long.toString(userId))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private void assertInvalidAccessToken(String authorizationHeader) throws Exception {
        var request = get("/api/v1/users/me");
        if (authorizationHeader != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"))
                .andExpect(jsonPath("$.message").value("유효한 인증이 필요합니다."));
    }

    private String extractJsonString(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int valueStart = json.indexOf(marker) + marker.length();
        assertTrue(valueStart >= marker.length());
        int valueEnd = json.indexOf('"', valueStart);
        return json.substring(valueStart, valueEnd);
    }
}
