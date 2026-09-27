package com.coworking.reservation.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.coworking.reservation.domain.entity.Space;
import com.coworking.reservation.domain.entity.SpaceType;
import com.coworking.reservation.domain.entity.User;
import com.coworking.reservation.domain.enums.ReservationStatus;
import com.coworking.reservation.domain.enums.UserStatus;
import com.coworking.reservation.integration.PostgreSqlTestContainerConfig;
import com.coworking.reservation.repository.SpaceRepository;
import com.coworking.reservation.repository.SpaceTypeRepository;
import com.coworking.reservation.repository.UserRepository;
import com.coworking.reservation.reservation.dto.CreateReservationRequest;
import com.coworking.reservation.reservation.service.ReservationService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ActiveProfiles("test")
class ReservationConcurrencyIntegrationTest extends PostgreSqlTestContainerConfig {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceTypeRepository spaceTypeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;
    private Space space;
    private Jwt userJwt;

    @BeforeEach
    void setUp() {
        cleanBusinessData();
        user = userRepository.save(new User(uniqueEmail(), "{noop}password", "Concurrent", "User", UserStatus.ACTIVE));
        SpaceType spaceType = spaceTypeRepository.findAllByOrderByIdAsc().getFirst();
        space = spaceRepository.save(new Space("Concurrent Room", 4, "Floor 2", new BigDecimal("30.00"), spaceType));
        userJwt = jwt(user.getId(), "USER");
    }

    @AfterEach
    void tearDown() {
        cleanBusinessData();
    }

    @Test
    void pessimisticSpaceLockAllowsOnlyOneConcurrentReservationForSameSlot() throws Exception {
        OffsetDateTime start = dateAt(10);
        OffsetDateTime end = dateAt(12);
        CreateReservationRequest request = new CreateReservationRequest(space.getId(), start, end);
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> createReservation = () -> {
            barrier.await(10, TimeUnit.SECONDS);
            try {
                reservationService.create(request, userJwt);
                return true;
            } catch (RuntimeException exception) {
                return false;
            }
        };

        Future<Boolean> first = executor.submit(createReservation);
        Future<Boolean> second = executor.submit(createReservation);

        long successes = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS))
                .stream()
                .filter(Boolean::booleanValue)
                .count();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(successes).isEqualTo(1);
        assertThat(countBlockingReservations(space.getId(), start, end)).isEqualTo(1);
    }

    private long countBlockingReservations(UUID spaceId, OffsetDateTime start, OffsetDateTime end) {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from reservations
                where space_id = ?
                  and status in (?, ?)
                  and start_time < ?
                  and end_time > ?
                """, Long.class, spaceId, ReservationStatus.PENDING_PAYMENT.getCode(), ReservationStatus.CONFIRMED.getCode(), end, start);
    }

    private void cleanBusinessData() {
        jdbcTemplate.update("delete from reservations");
        jdbcTemplate.update("delete from spaces");
        jdbcTemplate.update("delete from user_roles");
        jdbcTemplate.update("delete from users");
    }

    private static OffsetDateTime dateAt(int hour) {
        return OffsetDateTime.of(2027, 1, 10, hour, 0, 0, 0, ZoneOffset.UTC);
    }

    private static Jwt jwt(UUID userId, String... roles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(userId.toString())
                .claim("roles", List.of(roles))
                .build();
    }

    private static String uniqueEmail() {
        return "concurrent-" + UUID.randomUUID() + "@example.com";
    }
}
