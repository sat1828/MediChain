package com.medichain.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.medichain.domain.entity.DrugBatch;
import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.Enums.AlertSeverity;
import com.medichain.domain.entity.Enums.AlertType;
import com.medichain.domain.entity.StockAlert;
import com.medichain.domain.entity.Ward;
import com.medichain.domain.repository.DrugBatchRepository;
import com.medichain.domain.repository.DrugSKURepository;
import com.medichain.domain.repository.StockAlertRepository;
import com.medichain.domain.repository.StockTransactionRepository;
import com.medichain.domain.repository.WardRepository;
import com.medichain.messaging.producer.StockAlertProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AlertEvaluatorServiceTest {

    @Mock private StockTransactionRepository transactionRepository;
    @Mock private DrugBatchRepository drugBatchRepository;
    @Mock private DrugSKURepository drugSkuRepository;
    @Mock private WardRepository wardRepository;
    @Mock private StockAlertRepository stockAlertRepository;
    @Mock private StockAlertProducer stockAlertProducer;

    private AlertEvaluatorService alertEvaluator;

    private Ward ward;
    private DrugSKU drugSku;
    private DrugBatch batch;

    @BeforeEach
    void setUp() throws Exception {
        alertEvaluator = new AlertEvaluatorService(transactionRepository, drugBatchRepository,
            drugSkuRepository, wardRepository, stockAlertRepository, stockAlertProducer);

        Field expiryField = AlertEvaluatorService.class.getDeclaredField("expiryCriticalDays");
        expiryField.setAccessible(true);
        expiryField.set(alertEvaluator, 30);

        Field stockField = AlertEvaluatorService.class.getDeclaredField("stockThresholdDays");
        stockField.setAccessible(true);
        stockField.set(alertEvaluator, 30);

        ward = new Ward();
        ward.setId(UUID.randomUUID());
        ward.setName("ICU");

        drugSku = new DrugSKU();
        drugSku.setId(UUID.randomUUID());
        drugSku.setGenericName("Ceftriaxone 1g");

        batch = new DrugBatch();
        batch.setId(UUID.randomUUID());
        batch.setBatchNumber("BATCH001");
        batch.setQuantityOnHand(100);
        batch.setExpiryDate(LocalDate.now().plusDays(20));
    }

    @Test
    void evaluateStockout_shouldCreateCriticalAlertWhenBelowThreshold() {
        when(drugBatchRepository.getTotalStockByWardAndSku(any(), any(), any())).thenReturn(50);
        when(transactionRepository.getTotalConsumptionSince(any(), any(), any(), any())).thenReturn(450);

        alertEvaluator.evaluateStockout(ward, drugSku);

        var alertCaptor = ArgumentCaptor.forClass(StockAlert.class);
        verify(stockAlertRepository).save(alertCaptor.capture());
        verify(stockAlertProducer).publishStockAlert(any());

        var alert = alertCaptor.getValue();
        assertEquals(AlertType.STOCKOUT, alert.getAlertType());
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
        assertEquals(ward.getId(), alert.getWard().getId());
        assertEquals(drugSku.getId(), alert.getDrugSku().getId());
    }

    @Test
    void evaluateStockout_shouldNotCreateAlertWhenStockIsSufficient() {
        when(drugBatchRepository.getTotalStockByWardAndSku(any(), any(), any())).thenReturn(1000);
        when(transactionRepository.getTotalConsumptionSince(any(), any(), any(), any())).thenReturn(300);

        alertEvaluator.evaluateStockout(ward, drugSku);

        verify(stockAlertRepository, never()).save(any());
        verify(stockAlertProducer, never()).publishStockAlert(any());
    }

    @Test
    void evaluateStockout_shouldCreateCriticalAlertWhenDaysRemainingBelow15() {
        when(drugBatchRepository.getTotalStockByWardAndSku(any(), any(), any())).thenReturn(10);
        when(transactionRepository.getTotalConsumptionSince(any(), any(), any(), any())).thenReturn(900);

        alertEvaluator.evaluateStockout(ward, drugSku);

        var alertCaptor = ArgumentCaptor.forClass(StockAlert.class);
        verify(stockAlertRepository).save(alertCaptor.capture());

        assertEquals(AlertSeverity.CRITICAL, alertCaptor.getValue().getSeverity());
    }

    @Test
    void evaluateExpiry_shouldCreateAlertForNearExpiryBatch() {
        batch.setExpiryDate(LocalDate.now().plusDays(25));

        alertEvaluator.evaluateExpiry(batch, ward, drugSku);

        verify(stockAlertRepository).save(any());
        verify(stockAlertProducer).publishStockAlert(any());
    }

    @Test
    void evaluateExpiry_shouldNotCreateAlertForBatchWithFarExpiry() {
        batch.setExpiryDate(LocalDate.now().plusDays(90));

        alertEvaluator.evaluateExpiry(batch, ward, drugSku);

        verify(stockAlertRepository, never()).save(any());
        verify(stockAlertProducer, never()).publishStockAlert(any());
    }

    @Test
    void evaluateExpiry_shouldNotCreateAlertForAlreadyExpiredBatch() {
        batch.setExpiryDate(LocalDate.now().minusDays(5));

        alertEvaluator.evaluateExpiry(batch, ward, drugSku);

        verify(stockAlertRepository, never()).save(any());
    }
}
