package com.quietup.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.quietup.global.security.JwtTokenService;
import com.quietup.noise.dto.CreateNoiseAlertRequest;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoiseAlertIntegrationTest {

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

    @BeforeEach
    void cleanDatabase() {
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

    @Test
    void requiresAuthenticationAndAcceptsOnlyDirectionAndNoiseType() throws Exception {
        List<String> requestFields = Arrays.stream(CreateNoiseAlertRequest.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
        assertEquals(List.of("direction", "noiseType"), requestFields);

        mockMvc.perform(post("/api/v1/noise-alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/noise-alerts/received"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/noise-alerts/sent"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/noise-alerts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void appliesFlywayV4TablesConstraintsAndIndexes() {
        Integer tables = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN ('noise_alerts', 'noise_alert_responses')
                """, Integer.class);
        Integer constraints = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND constraint_name IN (
                    'fk_noise_alerts_apartment_complex',
                    'fk_noise_alerts_sender_residence',
                    'fk_noise_alerts_target_unit',
                    'uk_noise_alert_responses_alert_id',
                    'fk_noise_alert_responses_alert',
                    'fk_noise_alert_responses_responder'
                  )
                """, Integer.class);
        Integer indexes = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'noise_alerts'
                  AND index_name IN (
                    'idx_noise_alerts_sender_created',
                    'idx_noise_alerts_target_created',
                    'idx_noise_alerts_apartment_created'
                  )
                """, Integer.class);

        assertEquals(2, tables);
        assertEquals(6, constraints);
        assertEquals(3, indexes);
    }

    @Test
    void createsUpAndDownAlertsUsingServerCalculatedUnits() throws Exception {
        long senderUserId = insertUser("sender@example.com");
        long upUserId = insertUser("upstairs@example.com");
        long downUserId = insertUser("downstairs@example.com");
        long apartmentId = insertApartment("조용한아파트", "서울특별시 강남구 조용로 1");
        long buildingId = insertBuilding(apartmentId, "101");
        long senderUnitId = insertUnit(buildingId, "1203", 12, 3);
        long upUnitId = insertUnit(buildingId, "1303", 13, 3);
        long downUnitId = insertUnit(buildingId, "1103", 11, 3);
        long senderResidenceId = insertResidence(senderUserId, senderUnitId);
        insertResidence(upUserId, upUnitId);
        insertResidence(downUserId, downUnitId);

        createAlert(accessToken(senderUserId), "UP", "FOOTSTEPS")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.noiseAlertId").isNumber())
                .andExpect(jsonPath("$.targetLabel").value("위층 이웃"))
                .andExpect(jsonPath("$.direction").value("UP"))
                .andExpect(jsonPath("$.noiseType").value("FOOTSTEPS"))
                .andExpect(jsonPath("$.displayMessage").isString())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.senderResidenceId").doesNotExist())
                .andExpect(jsonPath("$.targetUnitId").doesNotExist())
                .andExpect(jsonPath("$.apartmentComplexId").doesNotExist());

        createAlert(accessToken(senderUserId), "DOWN", "FURNITURE")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetLabel").value("아래층 이웃"))
                .andExpect(jsonPath("$.direction").value("DOWN"))
                .andExpect(jsonPath("$.noiseType").value("FURNITURE"));

        List<AlertRow> alerts = jdbcTemplate.query("""
                SELECT apartment_complex_id, sender_residence_id, target_unit_id, direction, noise_type, status
                FROM noise_alerts
                ORDER BY id
                """, (resultSet, rowNumber) -> new AlertRow(
                resultSet.getLong("apartment_complex_id"),
                resultSet.getLong("sender_residence_id"),
                resultSet.getLong("target_unit_id"),
                resultSet.getString("direction"),
                resultSet.getString("noise_type"),
                resultSet.getString("status")));

        assertEquals(List.of(
                new AlertRow(apartmentId, senderResidenceId, upUnitId, "UP", "FOOTSTEPS", "SENT"),
                new AlertRow(apartmentId, senderResidenceId, downUnitId, "DOWN", "FURNITURE", "SENT")), alerts);
    }

    @Test
    void requiresVerifiedResidence() throws Exception {
        long userId = insertUser("unverified@example.com");

        createAlert(accessToken(userId), "UP", "MUSIC")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESIDENCE_REQUIRED"));
        mockMvc.perform(get("/api/v1/noise-alerts/received")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(userId))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESIDENCE_REQUIRED"));
    }

    @Test
    void returnsSameTargetUnavailableForMissingFloorUnitAndResidents() throws Exception {
        long missingFloorUser = insertUser("missing-floor@example.com");
        long firstApartment = insertApartment("첫번째아파트", "서울특별시 강남구 첫번째로 1");
        long firstBuilding = insertBuilding(firstApartment, "101");
        long firstSenderUnit = insertUnit(firstBuilding, "101", 1, 1);
        insertResidence(missingFloorUser, firstSenderUnit);

        long missingUnitUser = insertUser("missing-unit@example.com");
        long secondApartment = insertApartment("두번째아파트", "서울특별시 강남구 두번째로 2");
        long secondBuilding = insertBuilding(secondApartment, "201");
        long secondSenderUnit = insertUnit(secondBuilding, "1001", 10, 1);
        insertUnit(secondBuilding, "1102", 11, 2);
        insertResidence(missingUnitUser, secondSenderUnit);

        long noResidentUser = insertUser("no-resident@example.com");
        long thirdApartment = insertApartment("세번째아파트", "서울특별시 강남구 세번째로 3");
        long thirdBuilding = insertBuilding(thirdApartment, "301");
        long thirdSenderUnit = insertUnit(thirdBuilding, "502", 5, 2);
        insertUnit(thirdBuilding, "602", 6, 2);
        insertResidence(noResidentUser, thirdSenderUnit);

        assertTargetUnavailable(accessToken(missingFloorUser), "DOWN");
        assertTargetUnavailable(accessToken(missingUnitUser), "UP");
        assertTargetUnavailable(accessToken(noResidentUser), "UP");
        assertEquals(0, count("noise_alerts"));
    }

    @Test
    void isolatesHistoriesAndDetailsWithoutExposingIdentity() throws Exception {
        long senderUserId = insertUser("history-sender@example.com");
        long firstTargetUserId = insertUser("history-target-one@example.com");
        long secondTargetUserId = insertUser("history-target-two@example.com");
        long unrelatedUserId = insertUser("history-unrelated@example.com");
        long otherBuildingUserId = insertUser("history-building@example.com");
        long otherApartmentUserId = insertUser("history-apartment@example.com");

        long apartmentId = insertApartment("공동체아파트", "서울특별시 송파구 공동체로 1");
        long buildingId = insertBuilding(apartmentId, "101");
        long senderUnitId = insertUnit(buildingId, "1203", 12, 3);
        long targetUnitId = insertUnit(buildingId, "1303", 13, 3);
        long unrelatedUnitId = insertUnit(buildingId, "1204", 12, 4);
        long otherBuildingId = insertBuilding(apartmentId, "102");
        long otherBuildingUnitId = insertUnit(otherBuildingId, "1303", 13, 3);

        long otherApartmentId = insertApartment("다른아파트", "서울특별시 송파구 다른로 2");
        long otherApartmentBuildingId = insertBuilding(otherApartmentId, "101");
        long otherApartmentUnitId = insertUnit(otherApartmentBuildingId, "1303", 13, 3);

        insertResidence(senderUserId, senderUnitId);
        insertResidence(firstTargetUserId, targetUnitId);
        insertResidence(secondTargetUserId, targetUnitId);
        insertResidence(unrelatedUserId, unrelatedUnitId);
        insertResidence(otherBuildingUserId, otherBuildingUnitId);
        insertResidence(otherApartmentUserId, otherApartmentUnitId);

        createAlert(accessToken(senderUserId), "UP", "PET").andExpect(status().isCreated());
        long noiseAlertId = jdbcTemplate.queryForObject("SELECT id FROM noise_alerts", Long.class);

        assertReceivedVisible(accessToken(firstTargetUserId), noiseAlertId);
        assertReceivedVisible(accessToken(secondTargetUserId), noiseAlertId);
        assertReceivedEmpty(accessToken(unrelatedUserId));
        assertReceivedEmpty(accessToken(otherBuildingUserId));
        assertReceivedEmpty(accessToken(otherApartmentUserId));

        mockMvc.perform(get("/api/v1/noise-alerts/sent")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken(senderUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].noiseAlertId").value(noiseAlertId))
                .andExpect(jsonPath("$[0].targetLabel").value("위층 이웃"))
                .andExpect(jsonPath("$[0].senderResidenceId").doesNotExist())
                .andExpect(jsonPath("$[0].targetUnitId").doesNotExist());

        assertDetailVisible(accessToken(senderUserId), noiseAlertId, "위층 이웃");
        assertDetailVisible(accessToken(firstTargetUserId), noiseAlertId, "알림을 보낸 이웃");
        assertDetailVisible(accessToken(secondTargetUserId), noiseAlertId, "알림을 보낸 이웃");
        assertDetailHidden(accessToken(unrelatedUserId), noiseAlertId);
        assertDetailHidden(accessToken(otherBuildingUserId), noiseAlertId);
        assertDetailHidden(accessToken(otherApartmentUserId), noiseAlertId);
    }

    private ResultActions createAlert(String token, String direction, String noiseType) throws Exception {
        return mockMvc.perform(post("/api/v1/noise-alerts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "direction": "%s",
                          "noiseType": "%s"
                        }
                        """.formatted(direction, noiseType)));
    }

    private void assertTargetUnavailable(String token, String direction) throws Exception {
        createAlert(token, direction, "OTHER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.code").value("NOISE_ALERT_TARGET_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("현재 알림을 전달할 수 있는 대상이 없습니다."));
    }

    private void assertReceivedVisible(String token, long noiseAlertId) throws Exception {
        mockMvc.perform(get("/api/v1/noise-alerts/received")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].noiseAlertId").value(noiseAlertId))
                .andExpect(jsonPath("$[0].senderLabel").value("알림을 보낸 이웃"))
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].nickname").doesNotExist())
                .andExpect(jsonPath("$[0].residenceId").doesNotExist())
                .andExpect(jsonPath("$[0].unitId").doesNotExist())
                .andExpect(jsonPath("$[0].apartmentComplexId").doesNotExist())
                .andExpect(jsonPath("$[0].buildingNumber").doesNotExist())
                .andExpect(jsonPath("$[0].unitNumber").doesNotExist());
    }

    private void assertReceivedEmpty(String token) throws Exception {
        mockMvc.perform(get("/api/v1/noise-alerts/received")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private void assertDetailVisible(String token, long noiseAlertId, String counterpartLabel) throws Exception {
        mockMvc.perform(get("/api/v1/noise-alerts/{noiseAlertId}", noiseAlertId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noiseAlertId").value(noiseAlertId))
                .andExpect(jsonPath("$.counterpartLabel").value(counterpartLabel))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.nickname").doesNotExist())
                .andExpect(jsonPath("$.senderResidenceId").doesNotExist())
                .andExpect(jsonPath("$.targetUnitId").doesNotExist())
                .andExpect(jsonPath("$.apartmentComplexId").doesNotExist())
                .andExpect(jsonPath("$.buildingNumber").doesNotExist())
                .andExpect(jsonPath("$.unitNumber").doesNotExist());
    }

    private void assertDetailHidden(String token, long noiseAlertId) throws Exception {
        mockMvc.perform(get("/api/v1/noise-alerts/{noiseAlertId}", noiseAlertId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOISE_ALERT_NOT_FOUND"));
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
        if (!List.of("noise_alerts").contains(tableName)) {
            throw new IllegalArgumentException("허용되지 않은 테이블입니다.");
        }
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }

    private record AlertRow(
            long apartmentComplexId,
            long senderResidenceId,
            long targetUnitId,
            String direction,
            String noiseType,
            String status) {
    }
}
