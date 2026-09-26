package com.coworking.reservation.auth.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coworking.reservation.auth.dto.AuthResponse;
import com.coworking.reservation.auth.dto.LoginRequest;
import com.coworking.reservation.auth.dto.RegisterRequest;
import com.coworking.reservation.auth.dto.UserResponse;
import com.coworking.reservation.domain.entity.RevokedToken;
import com.coworking.reservation.domain.entity.Role;
import com.coworking.reservation.domain.entity.User;
import com.coworking.reservation.domain.enums.UserStatus;
import com.coworking.reservation.exception.AuthenticatedUserNotFoundException;
import com.coworking.reservation.exception.DuplicateEmailException;
import com.coworking.reservation.exception.MissingCatalogRoleException;
import com.coworking.reservation.repository.RevokedTokenRepository;
import com.coworking.reservation.repository.RoleRepository;
import com.coworking.reservation.repository.UserRepository;
import com.coworking.reservation.security.jwt.JwtTokenService;
import com.coworking.reservation.security.userdetails.CustomUserPrincipal;

@Service
public class AuthService {

    private static final String DEFAULT_USER_ROLE = "USER";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RevokedTokenRepository revokedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
            RevokedTokenRepository revokedTokenRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.revokedTokenRepository = revokedTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }

        Role userRole = roleRepository.findByCode(DEFAULT_USER_ROLE)
                .orElseThrow(() -> new MissingCatalogRoleException(DEFAULT_USER_ROLE));

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                UserStatus.ACTIVE
        );

        user.addRole(userRole);

        try {
            return UserResponse.from(userRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password())
        );

        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        String accessToken = jwtTokenService.createAccessToken(principal);
        return new AuthResponse(accessToken, TOKEN_TYPE, jwtTokenService.getAccessTokenTtlSeconds());
    }

    @Transactional
    public void logout(Jwt jwt) {
        UUID jti = UUID.fromString(jwt.getId());
        if (revokedTokenRepository.existsByJti(jti)) {
            return;
        }

        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(jwt.getExpiresAt(), ZoneOffset.UTC);
        try {
            revokedTokenRepository.save(new RevokedToken(jti, expiresAt));
        } catch (DataIntegrityViolationException ignored) {
            // Idempotent logout: another request already revoked this token.
        }
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(AuthenticatedUserNotFoundException::new);
        return UserResponse.from(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
