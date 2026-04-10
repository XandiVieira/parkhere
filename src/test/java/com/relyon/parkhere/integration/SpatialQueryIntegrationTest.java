package com.relyon.parkhere.integration;

import com.relyon.parkhere.model.ParkingSpot;
import com.relyon.parkhere.model.User;
import com.relyon.parkhere.model.enums.Role;
import com.relyon.parkhere.model.enums.SpotType;
import com.relyon.parkhere.repository.ParkingSpotRepository;
import com.relyon.parkhere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("isDockerAvailable")
class SpatialQueryIntegrationTest {

    static boolean isDockerAvailable() {
        // Check common Docker socket locations on macOS/Linux
        return Files.exists(Path.of("/var/run/docker.sock"))
                || Files.exists(Path.of(System.getProperty("user.home"), ".docker/run/docker.sock"))
                || System.getenv("DOCKER_HOST") != null;
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("parkhere_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private ParkingSpotRepository spotRepository;

    @Autowired
    private UserRepository userRepository;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private static final double CENTER_LAT = -30.0346;
    private static final double CENTER_LNG = -51.2177;

    private User testUser;

    @BeforeEach
    void setUp() {
        spotRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .name("Test User")
                .email("test@integration.com")
                .password("encoded")
                .role(Role.USER)
                .build());
    }

    private ParkingSpot createSpot(String name, double lat, double lng) {
        return spotRepository.save(ParkingSpot.builder()
                .name(name)
                .type(SpotType.STREET)
                .location(GF.createPoint(new Coordinate(lng, lat)))
                .createdBy(testUser)
                .build());
    }

    @Test
    void findWithinRadius_shouldReturnSpotsWithinDistance() {
        createSpot("Near Spot", CENTER_LAT + 0.0009, CENTER_LNG);
        createSpot("Far Spot", CENTER_LAT + 0.045, CENTER_LNG);

        var results = spotRepository.findWithinRadius(CENTER_LAT, CENTER_LNG, 500);

        assertEquals(1, results.size());
        assertEquals("Near Spot", results.getFirst().getName());
    }

    @Test
    void findWithinRadius_shouldReturnEmptyWhenNoneInRange() {
        createSpot("Very Far", CENTER_LAT + 1.0, CENTER_LNG);

        var results = spotRepository.findWithinRadius(CENTER_LAT, CENTER_LNG, 1000);

        assertTrue(results.isEmpty());
    }

    @Test
    void findWithinRadius_shouldReturnMultipleSpotsOrderedByDistance() {
        createSpot("Medium", CENTER_LAT + 0.003, CENTER_LNG);
        createSpot("Closest", CENTER_LAT + 0.0005, CENTER_LNG);
        createSpot("Farthest", CENTER_LAT + 0.008, CENTER_LNG);

        var results = spotRepository.findWithinRadius(CENTER_LAT, CENTER_LNG, 2000);

        assertEquals(3, results.size());
        assertEquals("Closest", results.get(0).getName());
        assertEquals("Medium", results.get(1).getName());
        assertEquals("Farthest", results.get(2).getName());
    }

    @Test
    void findWithinRadius_shouldExcludeInactiveSpots() {
        createSpot("Active", CENTER_LAT + 0.001, CENTER_LNG);
        var inactiveSpot = createSpot("Inactive", CENTER_LAT + 0.001, CENTER_LNG + 0.001);
        inactiveSpot.setActive(false);
        spotRepository.save(inactiveSpot);

        var results = spotRepository.findWithinRadius(CENTER_LAT, CENTER_LNG, 500);

        assertEquals(1, results.size());
        assertEquals("Active", results.getFirst().getName());
    }

    @Test
    void findWithinRadius_nearbyDetection50m() {
        createSpot("Existing", CENTER_LAT, CENTER_LNG);

        var nearby = spotRepository.findWithinRadius(CENTER_LAT + 0.00027, CENTER_LNG, 50);
        assertFalse(nearby.isEmpty());

        var farther = spotRepository.findWithinRadius(CENTER_LAT + 0.002, CENTER_LNG, 50);
        assertTrue(farther.isEmpty());
    }

    @Test
    void searchWithFilters_shouldFilterByType() {
        createSpot("Street Spot", CENTER_LAT + 0.001, CENTER_LNG);
        spotRepository.save(ParkingSpot.builder()
                .name("Mall Spot")
                .type(SpotType.MALL)
                .location(GF.createPoint(new Coordinate(CENTER_LNG, CENTER_LAT + 0.001)))
                .createdBy(testUser)
                .build());

        var results = spotRepository.searchWithFilters(
                CENTER_LAT, CENTER_LNG, 500,
                SpotType.MALL, null, null, null, null,
                PageRequest.of(0, 20));

        assertEquals(1, results.getTotalElements());
        assertEquals("Mall Spot", results.getContent().getFirst().getName());
    }

    @Test
    void searchWithFilters_shouldFilterByMaxPrice() {
        spotRepository.save(ParkingSpot.builder()
                .name("Cheap Spot")
                .type(SpotType.STREET)
                .location(GF.createPoint(new Coordinate(CENTER_LNG, CENTER_LAT + 0.001)))
                .priceMax(5.0)
                .createdBy(testUser)
                .build());

        spotRepository.save(ParkingSpot.builder()
                .name("Expensive Spot")
                .type(SpotType.STREET)
                .location(GF.createPoint(new Coordinate(CENTER_LNG + 0.001, CENTER_LAT + 0.001)))
                .priceMax(50.0)
                .createdBy(testUser)
                .build());

        var results = spotRepository.searchWithFilters(
                CENTER_LAT, CENTER_LNG, 2000,
                null, 10.0, null, null, null,
                PageRequest.of(0, 20));

        assertEquals(1, results.getTotalElements());
        assertEquals("Cheap Spot", results.getContent().getFirst().getName());
    }

    @Test
    void searchWithFilters_shouldSearchByNameQuery() {
        createSpot("Rua Lima e Silva", CENTER_LAT + 0.001, CENTER_LNG);
        createSpot("Terreno Beira Rio", CENTER_LAT + 0.002, CENTER_LNG);

        var results = spotRepository.searchWithFilters(
                CENTER_LAT, CENTER_LNG, 2000,
                null, null, null, null, "Lima",
                PageRequest.of(0, 20));

        assertEquals(1, results.getTotalElements());
        assertEquals("Rua Lima e Silva", results.getContent().getFirst().getName());
    }
}
