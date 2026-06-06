package com.medichain.domain.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record NgoRequestResponse(
    UUID id,
    UUID drugBatchId, String batchNumber,
    UUID ngoId, String ngoName,
    Integer quantityRequested, Integer quantityApproved,
    String status,
    String reviewedBy,
    String logisticsNotes,
    LocalDateTime pickupDate,
    LocalDateTime createdAt
) {}
