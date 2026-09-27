package com.coworking.reservation.reservation.service;

import com.coworking.reservation.domain.entity.Reservation;
import com.coworking.reservation.domain.entity.Space;
import com.coworking.reservation.domain.entity.User;
import com.coworking.reservation.domain.enums.ReservationStatus;
import com.coworking.reservation.exception.AuthenticatedUserNotFoundException;
import com.coworking.reservation.exception.FieldValidationException;
import com.coworking.reservation.exception.InvalidReservationTransitionException;
import com.coworking.reservation.exception.ReservationNotFoundException;
import com.coworking.reservation.exception.ReservationOverlapException;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.reservation.repository.SpaceRepository;
import com.coworking.reservation.repository.UserRepository;
import com.coworking.reservation.reservation.dto.CreateReservationRequest;
import com.coworking.reservation.reservation.dto.ReservationPageResponse;
import com.coworking.reservation.reservation.dto.ReservationResponse;
import com.coworking.reservation.reservation.dto.ReservationSearchCriteria;
import com.coworking.reservation.reservation.dto.ReservationSpaceResponse;
import com.coworking.reservation.reservation.dto.ReservationStatusResponse;
import com.coworking.reservation.reservation.dto.ReservationUserResponse;
import com.coworking.reservation.reservation.dto.UpdateReservationStatusRequest;
import com.coworking.reservation.reservation.state.ReservationState;
import com.coworking.reservation.reservation.state.ReservationStateResolver;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReservationService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final List<ReservationStatus> BLOCKING_STATUSES = List.of(
            ReservationStatus.PENDING_PAYMENT,
            ReservationStatus.CONFIRMED
    );

    private final ReservationRepository reservationRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final ReservationPriceCalculator priceCalculator;
    private final ReservationStateResolver stateResolver;

    public ReservationService(ReservationRepository reservationRepository, SpaceRepository spaceRepository,
            UserRepository userRepository, ReservationPriceCalculator priceCalculator,
            ReservationStateResolver stateResolver) {
        this.reservationRepository = reservationRepository;
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
        this.priceCalculator = priceCalculator;
        this.stateResolver = stateResolver;
    }

    @Transactional
    public ReservationResponse create(CreateReservationRequest request, Jwt jwt) {
        validateInterval(request.startTime(), request.endTime(), "endTime",
                "La fecha de finalización debe ser posterior a la fecha de inicio");

        UUID userId = authenticatedUserId(jwt);
        User user = userRepository.findById(userId)
                .orElseThrow(AuthenticatedUserNotFoundException::new);

        Space space = spaceRepository.findByIdForUpdate(request.spaceId())
                .orElseThrow(() -> new FieldValidationException(Map.of("spaceId", "El espacio no existe")));

        if (reservationRepository.existsBlockingOverlap(
                space.getId(), request.startTime(), request.endTime(), BLOCKING_STATUSES)) {
            throw new ReservationOverlapException();
        }

        BigDecimal totalPrice = priceCalculator.calculate(space.getHourlyRate(), request.startTime(), request.endTime());
        Reservation reservation = new Reservation(
                user,
                space,
                request.startTime(),
                request.endTime(),
                ReservationStatus.PENDING_PAYMENT,
                totalPrice
        );

        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public ReservationPageResponse findAll(ReservationSearchCriteria criteria, Jwt jwt) {
        validateSearchInterval(criteria.from(), criteria.to());
        ReservationStatus status = resolveStatus(criteria.statusCode());
        UUID authenticatedUserId = authenticatedUserId(jwt);
        boolean admin = isAdmin(jwt);
        UUID effectiveUserId = criteria.userId();

        if (!admin) {
            if (effectiveUserId != null && !effectiveUserId.equals(authenticatedUserId)) {
                throw new AccessDeniedException("Prohibido");
            }
            effectiveUserId = authenticatedUserId;
        }

        Specification<Reservation> specification = Specification
                .where(ReservationSpecifications.userId(effectiveUserId))
                .and(ReservationSpecifications.spaceId(criteria.spaceId()))
                .and(ReservationSpecifications.status(status))
                .and(ReservationSpecifications.intersects(criteria.from(), criteria.to()));

        Page<Reservation> page = reservationRepository.findAll(specification, pageable(criteria.page(), criteria.size()));
        return new ReservationPageResponse(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ReservationResponse findById(UUID id, Jwt jwt) {
        Reservation reservation = reservationRepository.findWithUserAndSpaceById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
        assertCanAccess(reservation, jwt);
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse updateStatus(UUID id, UpdateReservationStatusRequest request, Jwt jwt) {
        ReservationStatus targetStatus = resolveRequiredStatus(request.statusCode());
        Reservation reservation = reservationRepository.findWithUserAndSpaceById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));

        assertCanAccess(reservation, jwt);
        if (!isAdmin(jwt) && targetStatus != ReservationStatus.CANCELLED) {
            throw new AccessDeniedException("Prohibido");
        }

        ReservationState currentState = stateResolver.resolve(reservation.getStatus());
        if (currentState == null || !currentState.canTransitionTo(targetStatus)) {
            throw new InvalidReservationTransitionException();
        }

        reservation.changeStatus(targetStatus);
        return toResponse(reservation);
    }

    private void validateSearchInterval(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null) {
            validateInterval(from, to, "to", "La fecha final del filtro debe ser posterior a la fecha inicial");
        }
    }

    private void validateInterval(OffsetDateTime startTime, OffsetDateTime endTime, String field, String message) {
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new FieldValidationException(Map.of(field, message));
        }
    }

    private ReservationStatus resolveStatus(Short statusCode) {
        if (statusCode == null) {
            return null;
        }
        return resolveRequiredStatus(statusCode);
    }

    private ReservationStatus resolveRequiredStatus(Short statusCode) {
        try {
            return ReservationStatus.fromCode(statusCode);
        } catch (IllegalArgumentException exception) {
            throw new FieldValidationException(Map.of("statusCode", "El estado de reserva no existe"));
        }
    }

    private Pageable pageable(Integer page, Integer size) {
        int pageNumber = page == null ? DEFAULT_PAGE : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;
        Map<String, String> fields = new java.util.HashMap<>();

        if (pageNumber < 0) {
            fields.put("page", "La página debe ser mayor o igual a 0");
        }
        if (pageSize < 1 || pageSize > MAX_SIZE) {
            fields.put("size", "El tamaño de página debe estar entre 1 y 100");
        }
        if (!fields.isEmpty()) {
            throw new FieldValidationException(fields);
        }

        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private void assertCanAccess(Reservation reservation, Jwt jwt) {
        if (!isAdmin(jwt) && !reservation.getUser().getId().equals(authenticatedUserId(jwt))) {
            throw new AccessDeniedException("Prohibido");
        }
    }

    private UUID authenticatedUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains(ADMIN_ROLE);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        User user = reservation.getUser();
        return new ReservationResponse(
                reservation.getId(),
                new ReservationUserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName()),
                new ReservationSpaceResponse(reservation.getSpace().getId(), reservation.getSpace().getName()),
                ReservationStatusResponse.from(reservation.getStatus()),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getTotalPrice(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
