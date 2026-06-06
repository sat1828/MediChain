package com.medichain.domain.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DashboardResponse(
    InventoryHealthMatrix inventoryHealth,
    List<ExpiryTimelineItem> expiryTimeline,
    List<ForecastSummary> topStockoutRisks,
    List<AlertResponse> activeAlerts,
    ProcurementPipeline procurementPipeline,
    Map<String, Long> alertCountsBySeverity
) {
    public record InventoryHealthMatrix(
        List<String> wardNames,
        List<String> drugNames,
        List<List<CellStatus>> cells
    ) {
        public record CellStatus(
            String status,
            int daysRemaining,
            int stockLevel
        ) {}
    }

    public record ExpiryTimelineItem(
        UUID batchId,
        String batchNumber,
        String drugName,
        String strength,
        String wardName,
        String shelfCode,
        LocalDate expiryDate,
        int daysToExpiry,
        String severity,
        int quantityOnHand
    ) {}

    public record ForecastSummary(
        UUID forecastId,
        String wardName,
        String drugName,
        String strength,
        LocalDate predictedStockoutDate,
        int daysToStockout,
        double confidenceScore,
        String vedClassification,
        boolean actionable
    ) {}

    public record ProcurementPipeline(
        int draftCount,
        int pendingApprovalCount,
        int approvedCount,
        int dispatchedCount,
        int receivedCount,
        List<ProcurementOrderResponse> recentOrders
    ) {}
}
