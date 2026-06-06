package com.medichain.domain.service;

import com.medichain.domain.dto.request.LoginRequest;
import com.medichain.domain.dto.response.LoginResponse;
import com.medichain.domain.entity.User;
import com.medichain.domain.repository.UserRepository;
import com.medichain.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.medichain.domain.entity.Enums.UserRole;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final EntityManager entityManager;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        var user = userRepository.findByUsernameWithAssociations(request.username())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        var permissions = getPermissionsForRole(user.getRole().name());
        var accessToken = jwtTokenProvider.generateAccessToken(
            user.getId(), user.getUsername(), user.getRole().name(),
            user.getHospital().getId(),
            user.getWard() != null ? user.getWard().getId() : null,
            permissions);

        var refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return new LoginResponse(
            accessToken, refreshToken, "Bearer", 28800000L,
            new LoginResponse.UserInfo(
                user.getId(), user.getUsername(), user.getFullName(),
                user.getRole().name(), user.getHospital().getId(),
                user.getWard() != null ? user.getWard().getId() : null,
                user.getWard() != null ? user.getWard().getName() : null));
    }

    @Transactional
    public void createUser(String username, String password, String fullName,
                            String email, String role, UUID hospitalId,
                            UUID wardId) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        var hospital = entityManager.getReference(com.medichain.domain.entity.Hospital.class, hospitalId);
        var ward = wardId != null ? entityManager.getReference(com.medichain.domain.entity.Ward.class, wardId) : null;

        var user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(UserRole.valueOf(role));
        user.setHospital(hospital);
        user.setWard(ward);
        user.setMustChangePassword(true);
        userRepository.save(user);
    }

    public LoginResponse refreshToken(UUID userId) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        var permissions = getPermissionsForRole(user.getRole().name());
        var accessToken = jwtTokenProvider.generateAccessToken(
            user.getId(), user.getUsername(), user.getRole().name(),
            user.getHospital().getId(),
            user.getWard() != null ? user.getWard().getId() : null,
            permissions);
        var refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        return new LoginResponse(accessToken, refreshToken, "Bearer", 28800000L,
            new LoginResponse.UserInfo(user.getId(), user.getUsername(), user.getFullName(),
                user.getRole().name(), user.getHospital().getId(),
                user.getWard() != null ? user.getWard().getId() : null,
                user.getWard() != null ? user.getWard().getName() : null));
    }

    private Set<String> getPermissionsForRole(String role) {
        return switch (role) {
            case "ADMIN" -> Set.of("ALL");
            case "PHARMACY_MANAGER" -> Set.of("VIEW_ALL_WARDS", "MANAGE_PROCUREMENT", "MANAGE_NGO",
                                                "VIEW_FORECASTS", "ACKNOWLEDGE_ALERTS", "GENERATE_REPORTS");
            case "WARD_PHARMACIST" -> Set.of("VIEW_OWN_WARD", "DISPENSE", "RECEIVE_STOCK", "VIEW_ALERTS");
            case "PROCUREMENT_OFFICER" -> Set.of("MANAGE_PROCUREMENT", "VIEW_FORECASTS", "VIEW_VENDORS");
            case "NGO_PARTNER" -> Set.of("VIEW_REDISTRIBUTION", "REQUEST_TRANSFER");
            default -> Set.of();
        };
    }
}
