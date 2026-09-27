package com.coworking.reservation.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.coworking.reservation.domain.entity.Reservation;
import com.coworking.reservation.domain.entity.Space;
import com.coworking.reservation.domain.entity.SpaceType;
import com.coworking.reservation.domain.entity.User;
import com.coworking.reservation.domain.enums.ReservationStatus;
import com.coworking.reservation.domain.enums.UserStatus;
import com.coworking.reservation.exception.ReservationOverlapException;
import com.coworking.reservation.integration.PostgreSqlTestContainerConfig;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.reservation.repository.SpaceRepository;
import com.coworking.reservation.repository.SpaceTypeRepository;
import com.coworking.reservation.repository.UserRepository;
import com.coworking.reservation.reservation.dto.CreateReservationRequest;
import com.coworking.reservation.reservation.dto.ReservationResponse;
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

@SpringBootTest
@ActiveProfiles("test")
class ReservationCreationIntegrationTest extends PostgreSqlTestContainerConfig {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

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
        user = userRepository.save(new User(uniqueEmail(), "{noop}password", "Test", "User", UserStatus.ACTIVE));
        SpaceType spaceType = spaceTypeRepository.findAllByOrderByIdAsc().getFirst();
        space = spaceRepository.save(new Space("Integration Room", 6, "Floor 1", new BigDecimal("20.00"), spaceType));
        userJwt = jwt(user.getId(), "USER");
    }

    @AfterEach
    void tearDown() {
        cleanBusinessData();
    }

    @Test
    void createsValidReservationWithPendingPaymentStatus() {
        OffsetDateTime start = dateAt(10);
        OffsetDateTime end = dateAt(12);

        ReservationResponse response = reservationService.create(new CreateReservationRequest(space.getId(), start, end), userJwt);

        assertThat(response.id()).isNotNull();
        assertThat(response.status().code()).isEqualTo(ReservationStatus.PENDING_PAYMENT.getCode());
        assertThat(countBlockingReservations(space.getId(), start, end)).isEqualTo(1);
    }

    @Test
    void rejectsOverlappingReservationAndKeepsOnlyExistingBlockingReservation() {
        reservationService.create(new CreateReservationRequest(space.getId(), dateAt(10), dateAt(12)), userJwt);

        assertThatThrownBy(() -> reservationService.create(
                new CreateReservationRequest(space.getId(), dateAt(11), dateAt(13)), userJwt))
                .isInstanceOf(ReservationOverlapException.class);

        assertThat(countBlockingReservations(space.getId(), dateAt(10), dateAt(13))).isEqualTo(1);
    }

    @Test
    void allowsAdjacentReservationAtBoundary() {
        reservationService.create(new CreateReservationRequest(space.getId(), dateAt(10), dateAt(12)), userJwt);

        ReservationResponse adjacent = reservationService.create(
                new CreateReservationRequest(space.getId(), dateAt(12), dateAt(14)), userJwt);

        assertThat(adjacent.status().code()).isEqualTo(ReservationStatus.PENDING_PAYMENT.getCode());
        assertThat(countBlockingReservations(space.getId(), dateAt(10), dateAt(14))).isEqualTo(2);
    }

    @Test
    void cancelledReservationDoesNotBlockSameTimeSlot() {
        reservationRepository.saveAndFlush(new Reservation(
                user,
                space,
                dateAt(10),
                dateAt(12),
                ReservationStatus.CANCELLED,
                new BigDecimal("40.00")
        ));

        ReservationResponse response = reservationService.create(
                new CreateReservationRequest(space.getId(), dateAt(10), dateAt(12)), userJwt);

        assertThat(response.status().code()).isEqualTo(ReservationStatus.PENDING_PAYMENT.getCode());
        assertThat(countBlockingReservations(space.getId(), dateAt(10), dateAt(12))).isEqualTo(1);
        assertThat(reservationRepository.findAll()).hasSize(2);
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
        return "reservation-" + UUID.randomUUID() + "@example.com";
    }
}
