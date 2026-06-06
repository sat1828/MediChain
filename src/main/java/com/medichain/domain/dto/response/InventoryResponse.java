package com.medichain.domain.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record InventoryResponse(
    UUID wardId,
    String wardName,
    java.util.List<DrugStock> drugs
) {
    public record DrugStock(
        UUID drugSkuId,
        String genericName,
        String brandName,
        String strength,
        String form,
        int totalQuantity,
        int batchCount,
        String vedClassification,
        String abcClassification,
        java.util.List<BatchDetail> batches
    ) {}

    public record BatchDetail(
        UUID batchId,
        String batchNumber,
        String shelfCode,
        int quantityOnHand,
        LocalDate expiryDate,
        int daysToExpiry,
        String expiryStatus,
        java.math.BigDecimal unitCost,
        java.math.BigDecimal mrp
    ) {}
}
