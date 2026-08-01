package com.quietup.residence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.quietup.global.security.JwtTokenService;
import com.quietup.residence.repository.ResidenceRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResidenceVerificationIntegrationTest {

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
    JwtTokenService jwtTokenService;

    @MockitoSpyBean
    ResidenceRepository residenceRepository;

    private ExecutorService executor;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM residences");
        jdbcTemplate.update("DELETE FROM residence_verification_codes");
        jdbcTemplate.update("DELETE FROM apartment_units");
        jdbcTemplate.update("DELETE FROM apartment_buildings");
        jdbcTemplate.update("DELETE FROM apartment_complexes");
        jdbcTemplate.update("DELETE FROM refresh_tokens");
        jdbcTemplate.update("DELETE FROM users");
    }

    @AfterEach
    void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void requiresAuthenticationForResidenceVerificationAndStatus() throws Exception {
        mockMvc.perform(post("/api/v1/residences/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));

        mockMvc.perform(get("/api/v1/residences/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void verifiesResidenceConsumesOnlyHashAndReturnsOwnStatus() throws Exception {
        long userId = insertUser("resident@example.com");
        UnitFixture unit = insertUnit("조용한아파트", "서울특별시 강남구 조용로 1", "101", "1203", 12, 3);
        String rawCode = "abcd-1234";
        String codeHash = insertCode(unit.unitId(), rawCode, LocalDateTime.now(ZoneOffset.UTC).plusDays(1), null);

        verify(accessToken(userId), unit, " ABCD-1234 ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.apartmentName").value("조용한아파트"))
                .andExpect(jsonPath("$.buildingNumber").value("101"))
                .andExpect(jsonPath("$.unitNumber").value("1203"))
                .andExpect(jsonPath("$.verifiedAt").isString())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.residenceId").doesNotExist())
                .andExpect(jsonPath("$.unitId").doesNotExist());

        assertEquals(1, count("residences"));
        assertEquals(userId, jdbcTemplate.queryForObject(
                "SELECT user_id FROM residences",
                Long.class));
        assertEquals(unit.unitId(), jdbcTemplate.queryForObject(
                "SELECT unit_id FROM residences",
                Long.class));
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT used_at FROM residence_verification_codes WHERE code_hash = ?",
                Timestamp.class,
                codeHash));
        assertEquals(userId, jdbcTemplate.queryForObject(
                "SELECT used_by_user_id FROM residence_verification_codes WHERE code_hash = ?",
                Long.class,
                codeHash));
        assertNotEquals(rawCode, codeHash);
        assertTrue(codeHash.matches("[0-9a-f]{64}"));
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM residence_verification_codes WHERE code_hash = ?",
                Integer.class,
                rawCode));

        myResidence(accessToken(userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.apartmentName").value("조용한아파트"))
                .andExpect(jsonPath("$.roadAddress").value("서울특별시 강남구 조용로 1"))
                .andExpect(jsonPath("$.buildingNumber").value("101"))
                .andExpect(jsonPath("$.unitNumber").value("1203"))
                .andExpect(jsonPath("$.verifiedAt").isString())
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.residenceId").doesNotExist())
                .andExpect(jsonPath("$.unitId").doesNotExist());
    }

    @Test
    void returnsOnlyUnverifiedStatusBeforeResidenceVerification() throws Exception {
        long userId = insertUser("unverified@example.com");

        myResidence(accessToken(userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.status").value("UNVERIFIED"))
                .andExpect(jsonPath("$.apartmentName").doesNotExist())
                .andExpect(jsonPath("$.unitNumber").doesNotExist());
    }

    @Test
    void returnsSameErrorForInvalidExpiredUsedWrongUnitMissingUnitAndMalformedCodes() throws Exception {
        UnitFixture requestedUnit = insertUnit(
                "조용한아파트", "서울특별시 강남구 조용로 1", "101", "1203", 12, 3);
        long otherBuildingId = insertBuilding(requestedUnit.apartmentId(), "102");
        long otherUnitId = insertUnit(otherBuildingId, "1301", 13, 1);
        LocalDateTime tomorrow = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);

        long unknownUser = insertUser("unknown-code@example.com");
        String expectedBody = invalidVerification(accessToken(unknownUser), requestedUnit, "UNKNOWN-01")
                .getResponse().getContentAsString(StandardCharsets.UTF_8);

        long expiredUser = insertUser("expired-code@example.com");
        insertCode(requestedUnit.unitId(), "EXPIRED-01", LocalDateTime.now(ZoneOffset.UTC).minusDays(1), null);
        assertEquals(expectedBody, invalidVerification(accessToken(expiredUser), requestedUnit, "EXPIRED-01")
                .getResponse().getContentAsString(StandardCharsets.UTF_8));

        long usedUser = insertUser("used-code@example.com");
        insertCode(requestedUnit.unitId(), "USED-CODE1", tomorrow, usedUser);
        assertEquals(expectedBody, invalidVerification(accessToken(usedUser), requestedUnit, "USED-CODE1")
                .getResponse().getContentAsString(StandardCharsets.UTF_8));

        long wrongUnitUser = insertUser("wrong-unit@example.com");
        insertCode(otherUnitId, "WRONG-UNIT", tomorrow, null);
        assertEquals(expectedBody, invalidVerification(accessToken(wrongUnitUser), requestedUnit, "WRONG-UNIT")
                .getResponse().getContentAsString(StandardCharsets.UTF_8));

        long missingUnitUser = insertUser("missing-unit@example.com");
        insertCode(requestedUnit.unitId(), "MISSING-01", tomorrow, null);
        UnitFixture missingUnit = new UnitFixture(
                requestedUnit.apartmentId(),
                requestedUnit.buildingNumber(),
                "9999",
                -1L);
        assertEquals(expectedBody, invalidVerification(accessToken(missingUnitUser), missingUnit, "MISSING-01")
                .getResponse().getContentAsString(StandardCharsets.UTF_8));

        long malformedUser = insertUser("malformed-code@example.com");
        assertEquals(expectedBody, invalidVerification(accessToken(malformedUser), requestedUnit, "BAD!")
                .getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsAlreadyVerifiedUserAndDoesNotConsumeAnotherCode() throws Exception {
        long userId = insertUser("verified@example.com");
        UnitFixture unit = insertUnit("조용한아파트", "서울특별시 강남구 조용로 1", "101", "1203", 12, 3);
        insertCode(unit.unitId(), "FIRST-001", LocalDateTime.now(ZoneOffset.UTC).plusDays(1), null);
        String secondHash = insertCode(
                unit.unitId(),
                "SECOND-01",
                LocalDateTime.now(ZoneOffset.UTC).plusDays(1),
                null);
        String token = accessToken(userId);

        verify(token, unit, "FIRST-001").andExpect(status().isCreated());
        verify(token, unit, "SECOND-01")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESIDENCE_ALREADY_VERIFIED"));

        assertCodeUnused(secondHash);
    }

    @Test
    void allowsMultipleUsersInSameUnitWithDifferentCodesWithoutExposingEachOther() throws Exception {
        long firstUserId = insertUser("family-one@example.com");
        long secondUserId = insertUser("family-two@example.com");
        UnitFixture unit = insertUnit("조용한아파트", "서울특별시 강남구 조용로 1", "101", "1203", 12, 3);
        insertCode(unit.unitId(), "FAMILY-001", LocalDateTime.now(ZoneOffset.UTC).plusDays(1), null);
        insertCode(unit.unitId(), "FAMILY-002", LocalDateTime.now(ZoneOffset.UTC).plusDays(1), null);

        verify(accessToken(firstUserId), unit, "FAMILY-001").andExpect(status().isCreated());
        verify(accessToken(secondUserId), unit, "FAMILY-002").andExpect(status().isCreated());

        assertEquals(2, count("residences"));
        myResidence(accessToken(firstUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.nickname").doesNotExist());
    }

    @Test
    void allowsOnlyOneConcurrentUseOfSameVerificationCode() throws Exception {
        long firstUserId = insertUser("concurrent-one@example.com");
        long secondUserId = insertUser("concurrent-two@example.com");
        UnitFixture unit = insertUnit("조용한아파트", "서울특별시 강남구 조용로 1", "101", "1203", 12, 3);
        String rawCode = "SHARED-001";
        String codeHash = insertCode(unit.unitId(), rawCode, LocalDateTime.now(ZoneOffset.UTC).plusDays(1), null);
        executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<VerificationAttempt> first = executor.submit(() -> performConcurrentVerification(
                accessToken(firstUserId), unit, rawCode, ready, start));
        Future<VerificationAttempt> second = executor.submit(() -> performConcurrentVerification(
                accessToken(secondUserId), unit, rawCode, ready, start));

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        List<VerificationAttempt> attempts = List.of(
                first.get(30, TimeUnit.SECONDS),
                second.get(30, TimeUnit.SECONDS));
        List<Integer> statuses = attempts.stream().map(VerificationAttempt::status).sorted().toList();

        assertEquals(List.of(201, 400), statuses);
        assertEquals(1, count("residences"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM residence_verification_codes WHERE code_hash = ? AND used_at IS NOT NULL",
                Integer.class,
                codeHash));
        assertTrue(attempts.stream()
                .filter(attempt -> attempt.status() == 400)
                .allMatch(attempt -> attempt.body().contains("\"code\":\"INVALID_RESIDENCE_VERIFICATION\"")));
    }

    @Test
    void allowsOnlyOneConcurrentResidenceForSameUserAndRollsBackFailedCodeConsumption() throws Exception {
        long userId = insertUser("same-user@example.com");
        UnitFixture unit = insertUnit("조용한아파트", "서울특별시 강남구 조용로 1", "101", "1203", 12, 3);
        String firstCode = "USER-CODE1";
        String secondCode = "USER-CODE2";
        String firstHash = insertCode(unit.unitId(), firstCode, LocalDateTime.now(ZoneOffset.UTC).plusDays(1), null);
        String secondHash = insertCode(unit.unitId(), secondCode, LocalDateTime.now(ZoneOffset.UTC).plusDays(1), null);
        CountDownLatch bothChecked = new CountDownLatch(2);
        doAnswer(invocation -> {
            boolean exists = jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM residences WHERE user_id = ?)",
                    Boolean.class,
                    userId);
            bothChecked.countDown();
            if (!bothChecked.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시 거주 여부 조회가 완료되지 않았습니다.");
            }
            return exists;
        }).when(residenceRepository).existsByUserId(eq(userId));

        executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        String token = accessToken(userId);
        Future<VerificationAttempt> first = executor.submit(() -> performConcurrentVerification(
                token, unit, firstCode, ready, start));
        Future<VerificationAttempt> second = executor.submit(() -> performConcurrentVerification(
                token, unit, secondCode, ready, start));

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        List<VerificationAttempt> attempts = List.of(
                first.get(30, TimeUnit.SECONDS),
                second.get(30, TimeUnit.SECONDS));
        List<Integer> statuses = attempts.stream().map(VerificationAttempt::status).sorted().toList();

        assertEquals(List.of(201, 409), statuses);
        assertEquals(1, count("residences"));
        VerificationAttempt failedAttempt = attempts.stream()
                .filter(attempt -> attempt.status() == 409)
                .findFirst()
                .orElseThrow();
        VerificationAttempt successfulAttempt = attempts.stream()
                .filter(attempt -> attempt.status() == 201)
                .findFirst()
                .orElseThrow();
        assertTrue(failedAttempt.body().contains("\"code\":\"RESIDENCE_ALREADY_VERIFIED\""));
        assertCodeUnused(failedAttempt.codeHash());
        assertCodeUsed(successfulAttempt.codeHash(), userId);
        assertEquals(List.of(firstHash, secondHash).stream().sorted().toList(),
                attempts.stream().map(VerificationAttempt::codeHash).sorted().toList());
    }

    private MvcResult invalidVerification(String token, UnitFixture unit, String code) throws Exception {
        return verify(token, unit, code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("INVALID_RESIDENCE_VERIFICATION"))
                .andExpect(jsonPath("$.message").value("거주 인증 정보를 확인할 수 없습니다."))
                .andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions verify(
            String token,
            UnitFixture unit,
            String code) throws Exception {
        return mockMvc.perform(post("/api/v1/residences/verify")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(verificationBody(unit, code)));
    }

    private org.springframework.test.web.servlet.ResultActions myResidence(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/residences/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private VerificationAttempt performConcurrentVerification(
            String token,
            UnitFixture unit,
            String code,
            CountDownLatch ready,
            CountDownLatch start) {
        try {
            ready.countDown();
            start.await();
            MvcResult result = verify(token, unit, code).andReturn();
            return new VerificationAttempt(
                    hash(code),
                    result.getResponse().getStatus(),
                    result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String verificationBody(UnitFixture unit, String code) {
        return """
                {
                  "apartmentId": %d,
                  "buildingNumber": "%s",
                  "unitNumber": "%s",
                  "verificationCode": "%s"
                }
                """.formatted(unit.apartmentId(), unit.buildingNumber(), unit.unitNumber(), code);
    }

    private long insertUser(String email) {
        jdbcTemplate.update("""
                INSERT INTO users (email, password_hash, nickname, created_at, updated_at)
                VALUES (?, 'unused-password-hash', '테스트사용자', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, email);
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private UnitFixture insertUnit(
            String apartmentName,
            String roadAddress,
            String buildingNumber,
            String unitNumber,
            int floorNumber,
            int lineNumber) {
        long apartmentId = insertApartment(apartmentName, roadAddress);
        long buildingId = insertBuilding(apartmentId, buildingNumber);
        long unitId = insertUnit(buildingId, unitNumber, floorNumber, lineNumber);
        return new UnitFixture(apartmentId, buildingNumber, unitNumber, unitId);
    }

    private long insertApartment(String name, String roadAddress) {
        jdbcTemplate.update("""
                INSERT INTO apartment_complexes (name, road_address, created_at, updated_at)
                VALUES (?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, name, roadAddress);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM apartment_complexes WHERE name = ? AND road_address = ?",
                Long.class,
                name,
                roadAddress);
    }

    private long insertBuilding(long apartmentId, String buildingNumber) {
        jdbcTemplate.update("""
                INSERT INTO apartment_buildings (apartment_complex_id, building_number, created_at)
                VALUES (?, ?, UTC_TIMESTAMP(6))
                """, apartmentId, buildingNumber);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM apartment_buildings WHERE apartment_complex_id = ? AND building_number = ?",
                Long.class,
                apartmentId,
                buildingNumber);
    }

    private long insertUnit(long buildingId, String unitNumber, int floorNumber, int lineNumber) {
        jdbcTemplate.update("""
                INSERT INTO apartment_units (building_id, unit_number, floor_number, line_number, created_at)
                VALUES (?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, buildingId, unitNumber, floorNumber, lineNumber);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM apartment_units WHERE building_id = ? AND unit_number = ?",
                Long.class,
                buildingId,
                unitNumber);
    }

    private String insertCode(
            long unitId,
            String rawCode,
            LocalDateTime expiresAt,
            Long usedByUserId) {
        String codeHash = hash(rawCode);
        jdbcTemplate.update("""
                INSERT INTO residence_verification_codes (
                    unit_id, code_hash, expires_at, used_at, used_by_user_id, created_at
                ) VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """,
                unitId,
                codeHash,
                Timestamp.valueOf(expiresAt),
                usedByUserId == null ? null : Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)),
                usedByUserId);
        return codeHash;
    }

    private String accessToken(long userId) {
        return jwtTokenService.issueAccessToken(userId).tokenValue();
    }

    private String hash(String rawCode) {
        String normalizedCode = rawCode.trim().toUpperCase(Locale.ROOT);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalizedCode.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private int count(String tableName) {
        if (!List.of("residences").contains(tableName)) {
            throw new IllegalArgumentException("허용되지 않은 테이블입니다.");
        }
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private void assertCodeUnused(String codeHash) {
        Timestamp usedAt = jdbcTemplate.queryForObject(
                "SELECT used_at FROM residence_verification_codes WHERE code_hash = ?",
                Timestamp.class,
                codeHash);
        Long usedByUserId = jdbcTemplate.queryForObject(
                "SELECT used_by_user_id FROM residence_verification_codes WHERE code_hash = ?",
                Long.class,
                codeHash);
        assertNull(usedAt);
        assertNull(usedByUserId);
    }

    private void assertCodeUsed(String codeHash, long userId) {
        Timestamp usedAt = jdbcTemplate.queryForObject(
                "SELECT used_at FROM residence_verification_codes WHERE code_hash = ?",
                Timestamp.class,
                codeHash);
        Long usedByUserId = jdbcTemplate.queryForObject(
                "SELECT used_by_user_id FROM residence_verification_codes WHERE code_hash = ?",
                Long.class,
                codeHash);
        assertNotNull(usedAt);
        assertEquals(userId, usedByUserId);
    }

    private record UnitFixture(
            long apartmentId,
            String buildingNumber,
            String unitNumber,
            long unitId) {
    }

    private record VerificationAttempt(String codeHash, int status, String body) {
    }
}
