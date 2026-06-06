package com.medichain.domain.dto.response;

import java.util.UUID;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserInfo user
) {
    public record UserInfo(
        UUID id,
        String username,
        String fullName,
        String role,
        UUID hospitalId,
        UUID wardId,
        String wardName
    ) {}
}
