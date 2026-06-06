package com.medichain.domain.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ForecastResponse(
    UUID id,
    UUID wardId,
    String wardName,
    UUID drugSkuId,
    String drugName,
    String strength,
    String vedClassification,
    LocalDate predictedStockoutDate,
    Integer recommendedOrderQuantity,
    Double confidenceScore,
    java.util.List<String> keyRiskFactors,
    java.util.List<TransferSuggestion> suggestedTransfers,
    LocalDateTime forecastGeneratedAt,
    boolean actionable
) {
    public record TransferSuggestion(
        UUID fromWardId,
        String fromWardName,
        UUID batchId,
        String batchNumber,
        int quantity
    ) {}
}
