package com.medichain.dashboard;

import com.medichain.domain.dto.response.AlertResponse;
import com.medichain.domain.dto.response.DashboardResponse;
import com.medichain.domain.dto.response.DashboardResponse.ExpiryTimelineItem;
import com.medichain.domain.dto.response.DashboardResponse.ForecastSummary;
import com.medichain.domain.dto.response.DashboardResponse.ProcurementPipeline;
import com.medichain.domain.dto.response.ProcurementOrderResponse;
import com.medichain.domain.entity.AiForecastReport;
import com.medichain.domain.entity.DrugBatch;
import com.medichain.domain.entity.Enums.ProcurementStatus;
import com.medichain.domain.repository.AiForecastReportRepository;
import com.medichain.domain.repository.DrugBatchRepository;
import com.medichain.domain.repository.ProcurementOrderRepository;
import com.medichain.domain.repository.StockAlertRepository;
import com.medichain.domain.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WardRepository wardRepository;
    private final StockAlertRepository stockAlertRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final AiForecastReportRepository forecastRepository;
    private final ProcurementOrderRepository procurementOrderRepository;

    @Cacheable(value = "dashboard", key = "#hospitalId")
    @Transactional(readOnly = true)
    public DashboardResponse getPharmacyManagerDashboard(UUID hospitalId) {
        var now = LocalDate.now();
        var wards = wardRepository.findByHospitalId(hospitalId);
        var wardNames = wards.stream().map(w -> w.getName()).toList();

        var allAlerts = stockAlertRepository.findActiveAlerts();
        var activeAlerts = allAlerts.stream()
            .map(a -> new AlertResponse(
                a.getId(), a.getAlertType(), a.getSeverity().name(), a.getMessage(),
                a.getWard().getId(), a.getWard().getName(),
                a.getDrugSku().getId(), a.getDrugSku().getGenericName(),
                a.getDrugBatch() != null ? a.getDrugBatch().getId() : null,
                a.getDrugBatch() != null ? a.getDrugBatch().getBatchNumber() : null,
                a.getCurrentStock(), a.getDaysUntilStockout(), a.getDaysUntilExpiry(),
                a.isAcknowledged(), a.getCreatedAt()))
            .toList();

        var alertCounts = allAlerts.stream()
            .collect(Collectors.groupingBy(
                a -> a.getSeverity().name(),
                Collectors.counting()));

        var expiryTimeline = buildExpiryTimeline(now);
        var topStockoutRisks = buildTopStockoutRisks();
        var procurementPipeline = buildProcurementPipeline();

        return new DashboardResponse(
            null, expiryTimeline, topStockoutRisks, activeAlerts,
            procurementPipeline, alertCounts);
    }

    private List<ExpiryTimelineItem> buildExpiryTimeline(LocalDate now) {
        var ninetyDaysFromNow = now.plusDays(90);
        var cutoffDate = now.plusDays(90);

        return drugBatchRepository.findExpiringBatchesWithDetails(now, cutoffDate)
            .stream()
            .filter(b -> b.getQuantityOnHand() > 0 && b.getShelf() != null)
            .map(b -> {
                var daysToExpiry = (int) ChronoUnit.DAYS.between(now, b.getExpiryDate());
                var severity = daysToExpiry < 30 ? "CRITICAL"
                    : daysToExpiry < 60 ? "WARNING" : "WATCH";
                return new ExpiryTimelineItem(
                    b.getId(), b.getBatchNumber(),
                    b.getDrugSku().getGenericName(),
                    b.getDrugSku().getStrength(),
                    b.getShelf().getWard().getName(),
                    b.getShelf().getShelfCode(),
                    b.getExpiryDate(), daysToExpiry, severity,
                    b.getQuantityOnHand());
            })
            .sorted(Comparator.comparingInt(ExpiryTimelineItem::daysToExpiry))
            .toList();
    }

    private List<ForecastSummary> buildTopStockoutRisks() {
        var since = LocalDateTime.now().minusDays(7);
        var forecasts = forecastRepository.findRecentForecastsWithDetails(since);
        return forecasts.stream()
            .filter(f -> f.getPredictedStockoutDate() != null)
            .sorted(Comparator.comparing(AiForecastReport::getPredictedStockoutDate))
            .limit(10)
            .map(f -> {
                var daysToStockout = (int) ChronoUnit.DAYS.between(LocalDate.now(), f.getPredictedStockoutDate());
                return new ForecastSummary(
                    f.getId(), f.getWard().getName(),
                    f.getDrugSku().getGenericName(),
                    f.getDrugSku().getStrength(),
                    f.getPredictedStockoutDate(), daysToStockout,
                    f.getConfidenceScore() != null ? f.getConfidenceScore() : 0.0,
                    f.getDrugSku().getVedClassification() != null
                        ? f.getDrugSku().getVedClassification().name() : "UNKNOWN",
                    f.isActionable());
            })
            .toList();
    }

    private ProcurementPipeline buildProcurementPipeline() {
        var counts = procurementOrderRepository.countByStatus().stream()
            .collect(Collectors.toMap(
                row -> (ProcurementStatus) row[0],
                row -> (Long) row[1]));

        var draftCount = counts.getOrDefault(ProcurementStatus.DRAFT, 0L).intValue();
        var pendingCount = counts.getOrDefault(ProcurementStatus.PENDING_APPROVAL, 0L).intValue();
        var approvedCount = counts.getOrDefault(ProcurementStatus.APPROVED, 0L).intValue();
        var dispatchedCount = counts.getOrDefault(ProcurementStatus.DISPATCHED, 0L).intValue();
        var receivedCount = counts.getOrDefault(ProcurementStatus.RECEIVED, 0L).intValue();

        var recentOrders = procurementOrderRepository.findAllOrderByDateDesc().stream()
            .limit(5)
            .map(o -> new ProcurementOrderResponse(
                o.getId(), o.getOrderNumber(), o.getStatus().name(),
                o.getVendor() != null ? o.getVendor().getName() : "N/A",
                o.getVendor() != null ? o.getVendor().getGstin() : null,
                o.getOrderDate(), o.getExpectedDeliveryDate(),
                o.getGeneratedBy() != null ? o.getGeneratedBy().getFullName() : "N/A",
                o.getApprovedBy() != null ? o.getApprovedBy().getFullName() : null,
                o.getTotalAmount(), o.getGstAmount(), o.getGrandTotal(),
                List.of(), o.getNotes(), "/api/v1/procurement/orders/" + o.getId() + "/pdf"))
            .toList();

        return new ProcurementPipeline(
            draftCount, pendingCount, approvedCount, dispatchedCount, receivedCount, recentOrders);
    }
}
