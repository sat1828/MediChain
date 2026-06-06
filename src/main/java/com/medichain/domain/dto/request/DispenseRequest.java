package com.medichain.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DispenseRequest(
    @NotNull UUID wardId,
    @NotNull UUID drugSkuId,
    @Min(1) int quantity,
    String notes
) {}
