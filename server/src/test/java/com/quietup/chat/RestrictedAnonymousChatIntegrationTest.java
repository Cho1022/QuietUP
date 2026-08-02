package com.quietup.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.quietup.global.security.JwtTokenService;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestrictedAnonymousChatIntegrationTest {

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

    private ExecutorService executor;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_rooms");
        jdbcTemplate.update("DELETE FROM noise_alert_responses");
        jdbcTemplate.update("DELETE FROM noise_alerts");
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
    void requiresAuthenticationAndAppliesFlywayV5Schema() throws Exception {
        mockMvc.perform(post("/api/v1/noise-alerts/1/chat-room"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/chat-rooms"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/chat-rooms/1"))
                .andExpect(status().isUnauthorized());

        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank",
                String.class);
        Integer tables = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN ('chat_rooms', 'chat_messages')
                """, Integer.class);
        Integer constraints = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND constraint_name IN (
                    'uk_chat_rooms_noise_alert_id',
                    'fk_chat_rooms_noise_alert',
                    'fk_chat_rooms_alert_sender',
                    'fk_chat_rooms_alert_responder',
                    'fk_chat_messages_room',
                    'fk_chat_messages_sender'
                  )
                """, Integer.class);
        Integer indexes = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND index_name IN (
                    'idx_chat_rooms_sender_opened',
                    'idx_chat_rooms_responder_opened',
                    'idx_chat_messages_room_id'
                  )
                """, Integer.class);
        Integer copiedIdentityColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name IN ('chat_rooms', 'chat_messages')
                  AND column_name IN (
                    'sender_user_id',
                    'responder_user_id',
                    'email',
                    'nickname',
                    'unit_number',
                    'building_number'
                  )
                """, Integer.class);

        assertEquals(List.of("1", "2", "3", "4", "5"), versions);
        assertEquals(2, tables);
        assertEquals(6, constraints);
        assertEquals(3, indexes);
        assertEquals(0, copiedIdentityColumns);
    }

    @Test
    void exposesRoomOnlyToAlertSenderAndFirstRequestChatResponder() throws Exception {
        ChatFixture fixture = createFixture("access", "REQUEST_CHAT", false);

        createRoom(accessToken(fixture.senderUserId()), fixture.noiseAlertId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chatRoomId").isNumber())
                .andExpect(jsonPath("$.noiseAlertId").value(fixture.noiseAlertId()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.counterpartLabel").value("알림을 받은 이웃"))
                .andExpect(jsonPath("$.openedAt").isString())
                .andExpect(jsonPath("$.senderUserId").doesNotExist())
                .andExpect(jsonPath("$.senderResidenceId").doesNotExist())
                .andExpect(jsonPath("$.responderResidenceId").doesNotExist())
                .andExpect(jsonPath("$.unitId").doesNotExist())
                .andExpect(jsonPath("$.apartmentComplexId").doesNotExist());

        long chatRoomId = jdbcTemplate.queryForObject(
                "SELECT id FROM chat_rooms WHERE noise_alert_id = ?",
                Long.class,
                fixture.noiseAlertId());
        assertRoomVisible(
                accessToken(fixture.senderUserId()),
                chatRoomId,
                fixture.noiseAlertId(),
                "알림을 받은 이웃");
        assertRoomVisible(
                accessToken(fixture.responderUserId()),
                chatRoomId,
                fixture.noiseAlertId(),
                "알림을 보낸 이웃");

        assertRoomListEmpty(accessToken(fixture.sameTargetUserId()));
        assertRoomHidden(accessToken(fixture.sameTargetUserId()), chatRoomId);
        assertRoomListEmpty(accessToken(fixture.unrelatedUserId()));
        assertRoomHidden(accessToken(fixture.unrelatedUserId()), chatRoomId);
        assertRoomListEmpty(accessToken(fixture.otherBuildingUserId()));
        assertRoomHidden(accessToken(fixture.otherBuildingUserId()), chatRoomId);
        assertRoomListEmpty(accessToken(fixture.otherApartmentUserId()));
        assertRoomHidden(accessToken(fixture.otherApartmentUserId()), chatRoomId);

        mockMvc.perform(get("/api/v1/chat-rooms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(fixture.unverifiedUserId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESIDENCE_REQUIRED"));
    }

    @Test
    void rejectsRoomCreationWithoutValidSenderAcceptedRequest() throws Exception {
        ChatFixture acknowledged = createFixture("ack", "ACKNOWLEDGED", false);
        createRoom(accessToken(acknowledged.senderUserId()), acknowledged.noiseAlertId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHAT_REQUEST_REQUIRED"));

        ChatFixture requested = createFixture("request", "REQUEST_CHAT", false);
        createRoom(accessToken(requested.responderUserId()), requested.noiseAlertId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));
        createRoom(accessToken(requested.sameTargetUserId()), requested.noiseAlertId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));
        createRoom(accessToken(requested.unrelatedUserId()), requested.noiseAlertId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));
        createRoom(accessToken(requested.senderUserId()), 999999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));

        ChatFixture resolved = createFixture("resolved", "REQUEST_CHAT", true);
        createRoom(accessToken(resolved.senderUserId()), resolved.noiseAlertId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHAT_REQUEST_REQUIRED"));
        assertEquals(0, count("chat_rooms"));
    }

    @Test
    void returnsExistingRoomIdempotently() throws Exception {
        ChatFixture fixture = createFixture("idempotent", "REQUEST_CHAT", false);

        createRoom(accessToken(fixture.senderUserId()), fixture.noiseAlertId())
                .andExpect(status().isCreated());
        long chatRoomId = jdbcTemplate.queryForObject(
                "SELECT id FROM chat_rooms WHERE noise_alert_id = ?",
                Long.class,
                fixture.noiseAlertId());

        createRoom(accessToken(fixture.senderUserId()), fixture.noiseAlertId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").value(chatRoomId))
                .andExpect(jsonPath("$.noiseAlertId").value(fixture.noiseAlertId()));
        assertEquals(1, count("chat_rooms"));
    }

    @Test
    void createsOneRoomForConcurrentRequests() throws Exception {
        ChatFixture fixture = createFixture("concurrent", "REQUEST_CHAT", false);
        String token = accessToken(fixture.senderUserId());
        executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<CreateAttempt> first = executor.submit(() -> performConcurrentCreate(
                token,
                fixture.noiseAlertId(),
                ready,
                start));
        Future<CreateAttempt> second = executor.submit(() -> performConcurrentCreate(
                token,
                fixture.noiseAlertId(),
                ready,
                start));

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        List<CreateAttempt> attempts = List.of(
                first.get(30, TimeUnit.SECONDS),
                second.get(30, TimeUnit.SECONDS));
        assertEquals(List.of(200, 201), attempts.stream().map(CreateAttempt::status).sorted().toList());
        assertEquals(1, count("chat_rooms"));

        long chatRoomId = jdbcTemplate.queryForObject(
                "SELECT id FROM chat_rooms WHERE noise_alert_id = ?",
                Long.class,
                fixture.noiseAlertId());
        assertTrue(attempts.stream().allMatch(attempt ->
                attempt.body().contains("\"chatRoomId\":" + chatRoomId)));
    }

    private ChatFixture createFixture(String key, String responseType, boolean resolved) throws Exception {
        long senderUserId = insertUser(key + "-sender@quietup.test");
        long responderUserId = insertUser(key + "-responder@quietup.test");
        long sameTargetUserId = insertUser(key + "-same-target@quietup.test");
        long unrelatedUserId = insertUser(key + "-unrelated@quietup.test");
        long otherBuildingUserId = insertUser(key + "-other-building@quietup.test");
        long otherApartmentUserId = insertUser(key + "-other-apartment@quietup.test");
        long unverifiedUserId = insertUser(key + "-unverified@quietup.test");

        long apartmentId = insertApartment(key + " 아파트", key + "로 1");
        long buildingId = insertBuilding(apartmentId, "101");
        long senderUnitId = insertUnit(buildingId, "1203", 12, 3);
        long targetUnitId = insertUnit(buildingId, "1303", 13, 3);
        long unrelatedUnitId = insertUnit(buildingId, "1204", 12, 4);
        insertResidence(senderUserId, senderUnitId);
        insertResidence(responderUserId, targetUnitId);
        insertResidence(sameTargetUserId, targetUnitId);
        insertResidence(unrelatedUserId, unrelatedUnitId);

        long otherBuildingId = insertBuilding(apartmentId, "102");
        long otherBuildingUnitId = insertUnit(otherBuildingId, "1303", 13, 3);
        insertResidence(otherBuildingUserId, otherBuildingUnitId);

        long otherApartmentId = insertApartment(key + " 다른 아파트", key + "로 2");
        long otherApartmentBuildingId = insertBuilding(otherApartmentId, "101");
        long otherApartmentUnitId = insertUnit(otherApartmentBuildingId, "1303", 13, 3);
        insertResidence(otherApartmentUserId, otherApartmentUnitId);

        createAlert(accessToken(senderUserId))
                .andExpect(status().isCreated());
        long noiseAlertId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM noise_alerts",
                Long.class);
        respond(accessToken(responderUserId), noiseAlertId, responseType)
                .andExpect(status().isCreated());
        if (resolved) {
            resolve(accessToken(senderUserId), noiseAlertId)
                    .andExpect(status().isNoContent());
        }

        return new ChatFixture(
                noiseAlertId,
                senderUserId,
                responderUserId,
                sameTargetUserId,
                unrelatedUserId,
                otherBuildingUserId,
                otherApartmentUserId,
                unverifiedUserId);
    }

    private ResultActions createAlert(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/noise-alerts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "direction": "UP",
                          "noiseType": "FOOTSTEPS"
                        }
                        """));
    }

    private ResultActions respond(String token, long noiseAlertId, String responseType) throws Exception {
        return mockMvc.perform(post("/api/v1/noise-alerts/{noiseAlertId}/responses", noiseAlertId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "responseType": "%s"
                        }
                        """.formatted(responseType)));
    }

    private ResultActions resolve(String token, long noiseAlertId) throws Exception {
        return mockMvc.perform(post("/api/v1/noise-alerts/{noiseAlertId}/resolve", noiseAlertId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions createRoom(String token, long noiseAlertId) throws Exception {
        return mockMvc.perform(post("/api/v1/noise-alerts/{noiseAlertId}/chat-room", noiseAlertId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private void assertRoomVisible(
            String token,
            long chatRoomId,
            long noiseAlertId,
            String counterpartLabel) throws Exception {
        mockMvc.perform(get("/api/v1/chat-rooms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].chatRoomId").value(chatRoomId))
                .andExpect(jsonPath("$[0].noiseAlertId").value(noiseAlertId))
                .andExpect(jsonPath("$[0].counterpartLabel").value(counterpartLabel))
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].residenceId").doesNotExist())
                .andExpect(jsonPath("$[0].unitId").doesNotExist())
                .andExpect(jsonPath("$[0].apartmentComplexId").doesNotExist());

        mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", chatRoomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatRoomId").value(chatRoomId))
                .andExpect(jsonPath("$.noiseAlertId").value(noiseAlertId))
                .andExpect(jsonPath("$.counterpartLabel").value(counterpartLabel))
                .andExpect(jsonPath("$.noiseType").value("FOOTSTEPS"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.nickname").doesNotExist())
                .andExpect(jsonPath("$.senderResidenceId").doesNotExist())
                .andExpect(jsonPath("$.responderResidenceId").doesNotExist())
                .andExpect(jsonPath("$.buildingNumber").doesNotExist())
                .andExpect(jsonPath("$.unitNumber").doesNotExist());
    }

    private void assertRoomListEmpty(String token) throws Exception {
        mockMvc.perform(get("/api/v1/chat-rooms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private void assertRoomHidden(String token, long chatRoomId) throws Exception {
        mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}", chatRoomId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHAT_ROOM_NOT_FOUND"));
    }

    private CreateAttempt performConcurrentCreate(
            String token,
            long noiseAlertId,
            CountDownLatch ready,
            CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 생성 시작 신호를 받지 못했습니다.");
        }
        MvcResult result = createRoom(token, noiseAlertId).andReturn();
        return new CreateAttempt(
                result.getResponse().getStatus(),
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private long insertUser(String email) {
        jdbcTemplate.update("""
                INSERT INTO users (email, password_hash, nickname, created_at, updated_at)
                VALUES (?, 'unused-password-hash', '테스트사용자', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, email);
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
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

    private long insertResidence(long userId, long unitId) {
        jdbcTemplate.update("""
                INSERT INTO residences (user_id, unit_id, verified_at, created_at, updated_at)
                VALUES (?, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
                """, userId, unitId);
        return jdbcTemplate.queryForObject("SELECT id FROM residences WHERE user_id = ?", Long.class, userId);
    }

    private String accessToken(long userId) {
        return jwtTokenService.issueAccessToken(userId).tokenValue();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private int count(String tableName) {
        if (!List.of("chat_rooms", "chat_messages").contains(tableName)) {
            throw new IllegalArgumentException("허용되지 않은 테이블입니다.");
        }
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private record ChatFixture(
            long noiseAlertId,
            long senderUserId,
            long responderUserId,
            long sameTargetUserId,
            long unrelatedUserId,
            long otherBuildingUserId,
            long otherApartmentUserId,
            long unverifiedUserId) {
    }

    private record CreateAttempt(int status, String body) {
    }
}
