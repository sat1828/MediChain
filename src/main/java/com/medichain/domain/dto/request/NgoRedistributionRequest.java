package com.medichain.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record NgoRedistributionRequest(
    @NotNull UUID drugBatchId,
    @NotNull UUID ngoId,
    @Min(1) int quantity,
    String logisticsNotes
) {}
