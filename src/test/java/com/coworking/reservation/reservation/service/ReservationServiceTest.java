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
import com.coworking.reservation.exception.FieldValidationException;
import com.coworking.reservation.payment.service.PaymentValidationResult;
import com.coworking.reservation.payment.service.PaymentValidationService;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.reservation.repository.SpaceRepository;
import com.coworking.reservation.repository.UserRepository;
import com.coworking.reservation.reservation.dto.CreateReservationRequest;
import com.coworking.reservation.reservation.dto.ReservationResponse;
import com.coworking.reservation.reservation.dto.UpdateReservationStatusRequest;
import com.coworking.reservation.reservation.event.ReservationConfirmedEvent;
import com.coworking.reservation.reservation.state.PendingPaymentReservationState;
import com.coworking.reservation.reservation.state.ReservationStateResolver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

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

    @Mock
    private PaymentValidationService paymentValidationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PlatformTransactionManager transactionManager;

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

    @Test
    void adminConfirmationWithApprovedPaymentTransitionsReservationToConfirmed() {
        ReservationService service = reservationServiceWithNoOpTransactions();
        UUID userId = UUID.randomUUID();
        User user = paymentUser(userId);
        Space space = org.mockito.Mockito.mock(Space.class);
        Reservation reservation = reservation(user, space, ReservationStatus.PENDING_PAYMENT);
        Jwt adminJwt = jwt(userId, "ADMIN");

        when(reservationRepository.findWithUserAndSpaceById(any())).thenReturn(Optional.of(reservation));
        when(stateResolver.resolve(ReservationStatus.PENDING_PAYMENT)).thenReturn(new PendingPaymentReservationState());
        when(paymentValidationService.validate(any())).thenReturn(PaymentValidationResult.approved());

        ReservationResponse response = service.updateStatus(UUID.randomUUID(),
                new UpdateReservationStatusRequest(ReservationStatus.CONFIRMED.getCode(), "test-approved"), adminJwt);

        assertThat(response.status().code()).isEqualTo(ReservationStatus.CONFIRMED.getCode());
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(eventPublisher).publishEvent(any(ReservationConfirmedEvent.class));
    }

    @Test
    void adminConfirmationWithUnavailablePaymentKeepsReservationPendingPayment() {
        ReservationService service = reservationServiceWithNoOpTransactions();
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        User user = paymentUser(userId);
        Space space = org.mockito.Mockito.mock(Space.class);
        Reservation reservation = reservation(user, space, ReservationStatus.PENDING_PAYMENT);
        Jwt adminJwt = jwt(userId, "ADMIN");

        when(reservationRepository.findWithUserAndSpaceById(any())).thenReturn(Optional.of(reservation));
        when(stateResolver.resolve(ReservationStatus.PENDING_PAYMENT)).thenReturn(new PendingPaymentReservationState());
        when(paymentValidationService.validate(any())).thenReturn(PaymentValidationResult.unavailable());

        ReservationResponse response = service.updateStatus(UUID.randomUUID(),
                new UpdateReservationStatusRequest(ReservationStatus.CONFIRMED.getCode(), "test-error"), adminJwt);

        assertThat(response.status().code()).isEqualTo(ReservationStatus.PENDING_PAYMENT.getCode());
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void adminConfirmationWithRejectedPaymentProducesSemanticErrorAndKeepsReservationPendingPayment() {
        ReservationService service = reservationServiceWithNoOpTransactions();
        UUID userId = UUID.randomUUID();
        User user = paymentUser(userId);
        Space space = org.mockito.Mockito.mock(Space.class);
        Reservation reservation = reservation(user, space, ReservationStatus.PENDING_PAYMENT);
        Jwt adminJwt = jwt(userId, "ADMIN");

        when(reservationRepository.findWithUserAndSpaceById(any())).thenReturn(Optional.of(reservation));
        when(stateResolver.resolve(ReservationStatus.PENDING_PAYMENT)).thenReturn(new PendingPaymentReservationState());
        when(paymentValidationService.validate(any())).thenReturn(PaymentValidationResult.rejected());

        assertThatThrownBy(() -> service.updateStatus(UUID.randomUUID(),
                new UpdateReservationStatusRequest(ReservationStatus.CONFIRMED.getCode(), "test-rejected"), adminJwt))
                .isInstanceOf(FieldValidationException.class)
                .satisfies(exception -> assertThat(((FieldValidationException) exception).getFields())
                        .containsEntry("paymentMethod", "El método de pago fue rechazado"));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING_PAYMENT);
        verify(eventPublisher, never()).publishEvent(any());
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

    private static Space responseSpace(UUID spaceId) {
        Space space = org.mockito.Mockito.mock(Space.class);
        when(space.getId()).thenReturn(spaceId);
        when(space.getName()).thenReturn("Room A");
        return space;
    }

    private static Space lockedSpace(UUID spaceId) {
        Space space = org.mockito.Mockito.mock(Space.class);
        when(space.getId()).thenReturn(spaceId);
        return space;
    }

    private static User paymentUser(UUID userId) {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@example.com");
        return user;
    }

    private ReservationService reservationServiceWithNoOpTransactions() {
        return new ReservationService(
                reservationRepository,
                spaceRepository,
                userRepository,
                priceCalculator,
                stateResolver,
                paymentValidationService,
                eventPublisher,
                new NoOpTransactionManager()
        );
    }

    private static Reservation reservation(User user, Space space, ReservationStatus status) {
        return new Reservation(
                user,
                space,
                OffsetDateTime.of(2027, 1, 10, 10, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2027, 1, 10, 12, 0, 0, 0, ZoneOffset.UTC),
                status,
                new BigDecimal("40.00")
        );
    }

    private static class NoOpTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
