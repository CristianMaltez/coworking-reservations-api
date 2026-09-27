package com.coworking.reservation.reservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coworking.reservation.domain.entity.Reservation;
import com.coworking.reservation.domain.entity.Space;
import com.coworking.reservation.domain.entity.User;
import com.coworking.reservation.domain.enums.ReservationStatus;
import com.coworking.reservation.exception.ReservationOverlapException;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.reservation.repository.SpaceRepository;
import com.coworking.reservation.repository.UserRepository;
import com.coworking.reservation.reservation.dto.CreateReservationRequest;
import com.coworking.reservation.reservation.state.ReservationStateResolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationPriceCalculator priceCalculator;

    @Mock
    private ReservationStateResolver stateResolver;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void createUsesSpaceLockChecksOverlapAndSavesPendingPaymentReservation() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.of(2027, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(2);
        Jwt jwt = jwt(userId, "USER");
        User user = responseUser(userId);
        Space lockedSpace = pricedResponseSpace(spaceId, new BigDecimal("20.00"));
        CreateReservationRequest request = new CreateReservationRequest(spaceId, start, end);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(spaceRepository.findByIdForUpdate(spaceId)).thenReturn(Optional.of(lockedSpace));
        when(reservationRepository.existsBlockingOverlap(eq(spaceId), eq(start), eq(end), any())).thenReturn(false);
        when(priceCalculator.calculate(new BigDecimal("20.00"), start, end)).thenReturn(new BigDecimal("40.00"));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.create(request, jwt);

        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(spaceRepository).findByIdForUpdate(spaceId);
        verify(reservationRepository).existsBlockingOverlap(eq(spaceId), eq(start), eq(end), eq(List.of(
                ReservationStatus.PENDING_PAYMENT,
                ReservationStatus.CONFIRMED
        )));
        verify(reservationRepository).save(reservationCaptor.capture());

        Reservation savedReservation = reservationCaptor.getValue();
        assertThat(savedReservation.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        assertThat(savedReservation.getUser()).isSameAs(user);
        assertThat(savedReservation.getSpace()).isSameAs(lockedSpace);
        assertThat(savedReservation.getTotalPrice()).isEqualByComparingTo("40.00");
    }

    @Test
    void createRejectsBlockingOverlapAndDoesNotSaveReservation() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.of(2027, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = start.plusHours(2);
        Jwt jwt = jwt(userId, "USER");
        User user = org.mockito.Mockito.mock(User.class);
        Space lockedSpace = lockedSpace(spaceId);
        CreateReservationRequest request = new CreateReservationRequest(spaceId, start, end);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(spaceRepository.findByIdForUpdate(spaceId)).thenReturn(Optional.of(lockedSpace));
        when(reservationRepository.existsBlockingOverlap(eq(spaceId), eq(start), eq(end), any())).thenReturn(true);

        assertThatThrownBy(() -> reservationService.create(request, jwt))
                .isInstanceOf(ReservationOverlapException.class);

        verify(spaceRepository).findByIdForUpdate(spaceId);
        verify(reservationRepository).existsBlockingOverlap(eq(spaceId), eq(start), eq(end), eq(List.of(
                ReservationStatus.PENDING_PAYMENT,
                ReservationStatus.CONFIRMED
        )));
        verify(reservationRepository, never()).save(any());
    }

    private static Jwt jwt(UUID userId, String... roles) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(userId.toString())
                .claim("roles", List.of(roles))
                .build();
    }

    private static User responseUser(UUID userId) {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getFirstName()).thenReturn("Test");
        when(user.getLastName()).thenReturn("User");
        return user;
    }

    private static Space pricedResponseSpace(UUID spaceId, BigDecimal hourlyRate) {
        Space space = org.mockito.Mockito.mock(Space.class);
        when(space.getId()).thenReturn(spaceId);
        when(space.getName()).thenReturn("Room A");
        when(space.getHourlyRate()).thenReturn(hourlyRate);
        return space;
    }

    private static Space lockedSpace(UUID spaceId) {
        Space space = org.mockito.Mockito.mock(Space.class);
        when(space.getId()).thenReturn(spaceId);
        return space;
    }
}
