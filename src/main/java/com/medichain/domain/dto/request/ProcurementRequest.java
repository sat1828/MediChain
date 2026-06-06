package com.medichain.domain.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ProcurementRequest(
    @NotNull UUID vendorId,
    @NotEmpty List<LineItem> lineItems,
    String notes
) {
    public record LineItem(
        @NotNull UUID drugSkuId,
        int requestedQuantity,
        java.math.BigDecimal unitPrice,
        String justification
    ) {}
}
