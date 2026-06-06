package com.medichain.domain.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    String transactionType,
    UUID drugBatchId,
    String batchNumber,
    String drugName,
    UUID wardId,
    String wardName,
    int quantity,
    int quantityBefore,
    int quantityAfter,
    String performedBy,
    LocalDateTime timestamp,
    String referenceNumber,
    String notes
) {}
