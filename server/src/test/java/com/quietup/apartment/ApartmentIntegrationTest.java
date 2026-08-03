package com.quietup.apartment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.quietup.global.security.JwtTokenService;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApartmentIntegrationTest {

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

    private String accessToken;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM residences");
        jdbcTemplate.update("DELETE FROM residence_verification_codes");
        jdbcTemplate.update("DELETE FROM apartment_units");
        jdbcTemplate.update("DELETE FROM apartment_buildings");
        jdbcTemplate.update("DELETE FROM apartment_complexes");
        accessToken = jwtTokenService.issueAccessToken(1L).tokenValue();
    }

    @Test
    void requiresAuthenticationForApartmentSearchAndBuildingLookup() throws Exception {
        mockMvc.perform(get("/api/v1/apartments").param("query", "조용"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));

        mockMvc.perform(get("/api/v1/apartments/1/buildings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_ACCESS_TOKEN"));
    }

    @Test
    void searchesApartmentsByNameAndRoadAddressWithoutResidentData() throws Exception {
        insertApartment("조용한아파트", "서울특별시 강남구 조용로 1");
        insertApartment("푸른마을", "서울특별시 조용구 평화로 2");
        insertApartment("다른마을", "부산광역시 바다로 3");

        mockMvc.perform(authenticatedGet("/api/v1/apartments").param("query", "조용"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].length()").value(3))
                .andExpect(jsonPath("$[0].apartmentId").isNumber())
                .andExpect(jsonPath("$[0].name").value("조용한아파트"))
                .andExpect(jsonPath("$[0].roadAddress").value("서울특별시 강남구 조용로 1"))
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].unitId").doesNotExist())
                .andExpect(jsonPath("$[0].residenceId").doesNotExist());
    }

    @Test
    void rejectsSearchQueryShorterThanTwoCharactersAfterTrimming() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/apartments"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(authenticatedGet("/api/v1/apartments").param("query", " 가 "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void limitsSearchToTwentyAndKeepsDeterministicOrder() throws Exception {
        for (int index = 24; index >= 0; index--) {
            insertApartment("검색단지%02d".formatted(index), "서울특별시 검색로 %d".formatted(index));
        }

        mockMvc.perform(authenticatedGet("/api/v1/apartments").param("query", "검색"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20))
                .andExpect(jsonPath("$[0].name").value("검색단지00"))
                .andExpect(jsonPath("$[19].name").value("검색단지19"));
    }

    @Test
    void returnsBuildingsOnlyForExistingApartment() throws Exception {
        long apartmentId = insertApartment("조용한아파트", "서울특별시 강남구 조용로 1");
        insertBuilding(apartmentId, "102");
        insertBuilding(apartmentId, "101");

        mockMvc.perform(authenticatedGet("/api/v1/apartments/{apartmentId}/buildings", apartmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].length()").value(2))
                .andExpect(jsonPath("$[0].buildingId").isNumber())
                .andExpect(jsonPath("$[0].buildingNumber").value("101"))
                .andExpect(jsonPath("$[0].unitId").doesNotExist())
                .andExpect(jsonPath("$[0].userId").doesNotExist())
                .andExpect(jsonPath("$[0].residentCount").doesNotExist());
    }

    @Test
    void returnsNotFoundForMissingApartmentBuildingLookup() throws Exception {
        mockMvc.perform(authenticatedGet("/api/v1/apartments/999999/buildings"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APARTMENT_NOT_FOUND"));
    }

    @Test
    void appliesFlywayV3TablesConstraintsAndIndexes() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank",
                String.class);
        Integer tables = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                    'apartment_complexes',
                    'apartment_buildings',
                    'apartment_units',
                    'residence_verification_codes',
                    'residences'
                  )
                """, Integer.class);
        Integer constraints = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND constraint_name IN (
                    'uk_apartment_complexes_name_address',
                    'uk_apartment_buildings_complex_number',
                    'fk_apartment_buildings_complex',
                    'uk_apartment_units_building_number',
                    'uk_apartment_units_building_floor_line',
                    'fk_apartment_units_building',
                    'uk_residence_verification_codes_hash',
                    'fk_residence_verification_codes_unit',
                    'fk_residence_verification_codes_user',
                    'uk_residences_user_id',
                    'fk_residences_user',
                    'fk_residences_unit'
                  )
                """, Integer.class);
        Integer indexes = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT CONCAT(table_name, ':', index_name))
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND (
                    (table_name = 'residence_verification_codes'
                      AND index_name IN (
                        'idx_residence_verification_codes_unit_id',
                        'idx_residence_verification_codes_expires_at'
                      ))
                    OR (table_name = 'residences' AND index_name = 'idx_residences_unit_id')
                  )
                """, Integer.class);
        Integer rawCodeColumns = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'residence_verification_codes'
                  AND column_name IN ('code', 'raw_code', 'verification_code')
                """, Integer.class);

        assertEquals(List.of("1", "2", "3", "4", "5"), versions);
        assertEquals(5, tables);
        assertEquals(12, constraints);
        assertEquals(3, indexes);
        assertEquals(0, rawCodeColumns);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedGet(
            String urlTemplate,
            Object... uriVariables) {
        return get(urlTemplate, uriVariables)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
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

    private void insertBuilding(long apartmentId, String buildingNumber) {
        jdbcTemplate.update("""
                INSERT INTO apartment_buildings (apartment_complex_id, building_number, created_at)
                VALUES (?, ?, UTC_TIMESTAMP(6))
                """, apartmentId, buildingNumber);
    }
}
