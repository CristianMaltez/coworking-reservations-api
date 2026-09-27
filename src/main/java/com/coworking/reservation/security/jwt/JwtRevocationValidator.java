package com.coworking.reservation.security.jwt;

import com.coworking.reservation.domain.enums.UserStatus;
import com.coworking.reservation.repository.RevokedTokenRepository;
import com.coworking.reservation.repository.UserRepository;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class JwtRevocationValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error("invalid_token", "Token inválido", null);

    private final RevokedTokenRepository revokedTokenRepository;
    private final UserRepository userRepository;

    public JwtRevocationValidator(RevokedTokenRepository revokedTokenRepository, UserRepository userRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        UUID jti = parseUuid(token.getId());
        UUID userId = parseUuid(token.getSubject());

        if (jti == null || userId == null || revokedTokenRepository.existsByJti(jti)) {
            return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
        }

        boolean active = userRepository.findById(userId)
                .map(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(false);

        return active ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
    }

    private UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
