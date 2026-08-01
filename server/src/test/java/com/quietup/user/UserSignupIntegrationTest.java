package com.quietup.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSignupIntegrationTest {

    private static final String SIGNUP_BODY = """
            {
              "email": " user@example.com ",
              "password": "password123",
              "nickname": " 조용한이웃 "
            }
            """;

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
    PasswordEncoder passwordEncoder;

    private ExecutorService executor;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @AfterEach
    void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void signsUpWithNormalizedEmailAndNicknameAndStoresBcryptHash() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SIGNUP_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.nickname").value("조용한이웃"));

        String storedEmail = jdbcTemplate.queryForObject("SELECT email FROM users", String.class);
        String storedPasswordHash = jdbcTemplate.queryForObject("SELECT password_hash FROM users", String.class);

        assertEquals("user@example.com", storedEmail);
        assertNotEquals("password123", storedPasswordHash);
        assertTrue(storedPasswordHash.startsWith("$2"));
        assertTrue(passwordEncoder.matches("password123", storedPasswordHash));
    }

    @Test
    void rejectsDuplicateEmailWithConflict() throws Exception {
        performSignup(SIGNUP_BODY);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "USER@EXAMPLE.COM",
                                  "password": "password456",
                                  "nickname": "다른이웃"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    void allowsOnlyOneOfConcurrentDuplicateSignups() throws Exception {
        executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Integer> first = executor.submit(() -> performConcurrentSignup(ready, start));
        Future<Integer> second = executor.submit(() -> performConcurrentSignup(ready, start));

        ready.await();
        start.countDown();

        List<Integer> statuses = List.of(first.get(), second.get()).stream().sorted().toList();
        assertEquals(List.of(201, 409), statuses);
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
    }

    @Test
    void rejectsInvalidSignupValues() throws Exception {
        assertValidationError("not-an-email", "password123", "조용한이웃", "email");
        assertValidationError("user@example.com", "pass1", "조용한이웃", "password");
        assertValidationError("user@example.com", "password", "조용한이웃", "password");
        assertValidationError("user@example.com", "password123", " a ", "nickname");
        assertFalse(jdbcTemplate.queryForObject("SELECT EXISTS(SELECT 1 FROM users)", Boolean.class));
    }

    private int performConcurrentSignup(CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            start.await();
            return performSignup(SIGNUP_BODY).getResponse().getStatus();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private org.springframework.test.web.servlet.MvcResult performSignup(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private void assertValidationError(String email, String password, String nickname, String field) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s",
                  "nickname": "%s"
                }
                """.formatted(email, password, nickname);

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == '%s')]", field).exists());
    }
}
