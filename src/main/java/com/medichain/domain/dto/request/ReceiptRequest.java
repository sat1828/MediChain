package com.medichain.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ReceiptRequest(
    @NotNull UUID wardId,
    @NotNull UUID drugSkuId,
    @NotNull UUID shelfId,
    @NotBlank String batchNumber,
    @NotNull LocalDate manufactureDate,
    @NotNull LocalDate expiryDate,
    @Min(1) int quantity,
    java.math.BigDecimal unitCost,
    java.math.BigDecimal mrp,
    String notes
) {}
