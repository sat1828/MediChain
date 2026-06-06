package com.medichain.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TransferRequest(
    @NotNull UUID sourceWardId,
    @NotNull UUID destinationWardId,
    @NotNull UUID drugBatchId,
    @Min(1) int quantity,
    String notes
) {}
