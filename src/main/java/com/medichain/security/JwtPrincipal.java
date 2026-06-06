package com.medichain.security;

import java.util.UUID;

public record JwtPrincipal(
    UUID userId,
    String username,
    String role,
    UUID hospitalId,
    UUID wardId
) {}
