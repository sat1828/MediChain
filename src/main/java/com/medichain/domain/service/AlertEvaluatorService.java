package com.medichain.domain.service;

import com.medichain.domain.entity.DrugBatch;
import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.Enums.AlertSeverity;
import com.medichain.domain.entity.Enums.AlertType;
import com.medichain.domain.entity.Enums.TransactionType;
import com.medichain.domain.entity.StockAlert;
import com.medichain.domain.entity.Ward;
import com.medichain.domain.repository.DrugBatchRepository;
import com.medichain.domain.repository.DrugSKURepository;
import com.medichain.domain.repository.StockAlertRepository;
import com.medichain.domain.repository.StockTransactionRepository;
import com.medichain.domain.repository.WardRepository;
import com.medichain.messaging.producer.StockAlertProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEvaluatorService {

    private final StockTransactionRepository transactionRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final DrugSKURepository drugSkuRepository;
    private final WardRepository wardRepository;
    private final StockAlertRepository stockAlertRepository;
    private final StockAlertProducer stockAlertProducer;

    @Value("${medichain.alerts.stock-threshold-days:30}")
    private int stockThresholdDays;

    @Value("${medichain.alerts.expiry-critical-days:30}")
    private int expiryCriticalDays;

    public void evaluateAndAlert(UUID wardId, UUID skuId, UUID batchId) {
        var ward = wardRepository.findById(wardId).orElse(null);
        var drugSku = drugSkuRepository.findById(skuId).orElse(null);
        var batch = drugBatchRepository.findById(batchId).orElse(null);
        if (ward == null || drugSku == null || batch == null) return;

        evaluateStockout(ward, drugSku);
        evaluateExpiry(batch, ward, drugSku);
    }

    void evaluateStockout(Ward ward, DrugSKU drugSku) {
        var now = LocalDate.now();
        var totalStock = drugBatchRepository.getTotalStockByWardAndSku(
            ward.getId(), drugSku.getId(), now);

        var ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        var totalConsumed = transactionRepository.getTotalConsumptionSince(
            ward.getId(), drugSku.getId(), TransactionType.DISPENSE, ninetyDaysAgo);

        var dailyAvg = totalConsumed / 90.0;
        if (dailyAvg <= 0) return;

        var daysRemaining = (int) (totalStock / dailyAvg);
        if (daysRemaining < stockThresholdDays) {
            var existingAlerts = stockAlertRepository.findActiveByWardSkuAndType(
                ward.getId(), drugSku.getId(), AlertType.STOCKOUT);
            if (!existingAlerts.isEmpty()) {
                log.debug("Unresolved STOCKOUT alert already exists for {} in {}, skipping duplicate",
                    drugSku.getGenericName(), ward.getName());
                return;
            }

            var alert = new StockAlert();
            alert.setWard(ward);
            alert.setDrugSku(drugSku);
            alert.setAlertType(AlertType.STOCKOUT);
            alert.setSeverity(daysRemaining < 15 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING);
            alert.setCurrentStock(totalStock);
            alert.setDaysUntilStockout(daysRemaining);
            alert.setMessage(String.format(
                "Stock low for %s in %s: ~%d days remaining (threshold: %d days)",
                drugSku.getGenericName(), ward.getName(), daysRemaining, stockThresholdDays));
            stockAlertRepository.save(alert);
            publishAfterCommit(alert);
            log.warn("STOCKOUT ALERT: {} in {} - {} days remaining", drugSku.getGenericName(),
                ward.getName(), daysRemaining);
        }
    }

    void evaluateExpiry(DrugBatch batch, Ward ward, DrugSKU drugSku) {
        if (batch.getExpiryDate() == null) return;
        var daysToExpiry = (int) ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());

        if (daysToExpiry < expiryCriticalDays && daysToExpiry >= 0) {
            var existingAlerts = stockAlertRepository.findActiveByWardSkuAndType(
                ward.getId(), drugSku.getId(), AlertType.EXPIRY_CRITICAL);
            if (!existingAlerts.isEmpty()) {
                log.debug("Unresolved EXPIRY_CRITICAL alert already exists for {} in {}, skipping duplicate",
                    drugSku.getGenericName(), ward.getName());
                return;
            }

            var alert = new StockAlert();
            alert.setWard(ward);
            alert.setDrugSku(drugSku);
            alert.setDrugBatch(batch);
            alert.setAlertType(AlertType.EXPIRY_CRITICAL);
            alert.setSeverity(AlertSeverity.CRITICAL);
            alert.setDaysUntilExpiry(daysToExpiry);
            alert.setCurrentStock(batch.getQuantityOnHand());
            alert.setMessage(String.format(
                "Batch %s of %s expires in %d days (%s) — %d units remaining",
                batch.getBatchNumber(), drugSku.getGenericName(),
                daysToExpiry, batch.getExpiryDate(), batch.getQuantityOnHand()));
            stockAlertRepository.save(alert);
            publishAfterCommit(alert);
            log.warn("EXPIRY ALERT: Batch {} of {} expires in {} days",
                batch.getBatchNumber(), drugSku.getGenericName(), daysToExpiry);
        }
    }

    private void publishAfterCommit(StockAlert alert) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        stockAlertProducer.publishStockAlert(alert);
                    }
                });
        } else {
            stockAlertProducer.publishStockAlert(alert);
        }
    }

    @Transactional
    public void acknowledgeAlert(UUID alertId, UUID userId) {
        var alert = stockAlertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.setAcknowledged(true);
        alert.setAcknowledgedById(userId);
        alert.setAcknowledgedAt(LocalDateTime.now());
        stockAlertRepository.save(alert);
    }
}
