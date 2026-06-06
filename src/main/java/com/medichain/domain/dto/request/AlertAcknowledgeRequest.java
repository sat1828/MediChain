package com.medichain.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AlertAcknowledgeRequest(
    @NotNull UUID alertId
) {}
