package com.medichain.domain.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlertResponse(
    UUID id,
    String alertType,
    String severity,
    String message,
    UUID wardId,
    String wardName,
    UUID drugSkuId,
    String drugName,
    UUID drugBatchId,
    String batchNumber,
    Integer currentStock,
    Integer daysUntilStockout,
    Integer daysUntilExpiry,
    boolean acknowledged,
    LocalDateTime createdAt
) {}
