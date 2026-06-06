package com.medichain.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProcurementOrderResponse(
    UUID id,
    String orderNumber,
    String status,
    String vendorName,
    String vendorGstin,
    LocalDate orderDate,
    LocalDate expectedDeliveryDate,
    String generatedBy,
    String approvedBy,
    BigDecimal totalAmount,
    BigDecimal gstAmount,
    BigDecimal grandTotal,
    List<LineItemResponse> lineItems,
    String notes,
    String pdfDownloadUrl
) {
    public record LineItemResponse(
        UUID lineItemId,
        String drugName,
        String strength,
        String hsnCode,
        int requestedQuantity,
        int approvedQuantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String justification
    ) {}
}
