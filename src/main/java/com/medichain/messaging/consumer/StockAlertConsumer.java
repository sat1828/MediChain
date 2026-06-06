package com.medichain.messaging.consumer;

import com.medichain.domain.entity.Enums.NGOTransferStatus;
import com.medichain.domain.entity.NGORedistributionRequest;
import com.medichain.domain.entity.StockAlert;
import com.medichain.domain.repository.NGORepository;
import com.medichain.domain.repository.NGORedistributionRequestRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockAlertConsumer {

    private final EntityManager entityManager;
    private final JavaMailSender mailSender;
    private final NGORepository ngoRepository;
    private final NGORedistributionRequestRepository redistributionRepository;

    @KafkaListener(topics = "stock.alerts.threshold", groupId = "medichain-alert-consumer",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumeStockAlert(StockAlert alert, Acknowledgment ack) {
        try {
            log.warn("STOCK ALERT: {} (severity: {})", alert.getMessage(), alert.getSeverity());

            if (alert.getDaysUntilStockout() != null && alert.getDaysUntilStockout() < 15) {
                sendEmailAlert("CRITICAL STOCKOUT",
                    "Drug: " + safeName(alert) + "\nMessage: " + alert.getMessage());
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process stock alert, acknowledging to avoid loop: {}", e.getMessage());
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "expiry.alerts.warning", groupId = "medichain-alert-consumer",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumeExpiryWarning(StockAlert alert, Acknowledgment ack) {
        try {
            log.info("EXPIRY WARNING: {} ({} days to expiry)", alert.getMessage(), alert.getDaysUntilExpiry());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process expiry warning, acknowledging: {}", e.getMessage());
            ack.acknowledge();
        }
    }

    @KafkaListener(topics = "expiry.alerts.critical", groupId = "medichain-alert-consumer",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumeExpiryCritical(StockAlert alert, Acknowledgment ack) {
        try {
            log.warn("CRITICAL EXPIRY: {} - {} units expiring", alert.getMessage(), alert.getCurrentStock());

            sendEmailAlert("CRITICAL DRUG EXPIRY",
                "Drug: " + safeName(alert) + "\nMessage: " + alert.getMessage());

            if (alert.getDrugBatch() != null && alert.getCurrentStock() != null
                && alert.getCurrentStock() > 0) {
                autoCreateNgoDraft(alert);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process critical expiry alert, acknowledging: {}", e.getMessage());
            ack.acknowledge();
        }
    }

    private String safeName(StockAlert alert) {
        try {
            return alert.getDrugSku() != null ? alert.getDrugSku().getGenericName() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private void sendEmailAlert(String subject, String body) {
        try {
            var message = new SimpleMailMessage();
            message.setTo("pharmacy-manager@demohospital.in");
            message.setSubject("[MediChain] " + subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email alert sent: {}", subject);
        } catch (Exception e) {
            log.warn("Failed to send email alert (mail not configured): {}", e.getMessage());
        }
    }

    private void autoCreateNgoDraft(StockAlert alert) {
        var verifiedNgos = ngoRepository.findVerifiedNgos();
        if (verifiedNgos.isEmpty()) {
            log.info("No verified NGOs found for auto-draft creation");
            return;
        }

        var ngo = verifiedNgos.getFirst();

        var existingRequests = redistributionRepository.findByDrugBatchId(alert.getDrugBatch().getId());
        var hasExistingDraft = existingRequests.stream()
            .anyMatch(r -> NGOTransferStatus.DRAFT.equals(r.getStatus()) || NGOTransferStatus.PENDING_APPROVAL.equals(r.getStatus()));
        if (hasExistingDraft) {
            log.info("Draft already exists for batch {}, skipping auto-create", alert.getDrugBatch().getBatchNumber());
            return;
        }

        var request = new NGORedistributionRequest();
        var batch = entityManager.getReference(com.medichain.domain.entity.DrugBatch.class, alert.getDrugBatch().getId());
        request.setDrugBatch(batch);
        request.setRequestingNgo(ngo);
        request.setQuantityRequested(Math.min(alert.getCurrentStock(), 100));
        request.setStatus(NGOTransferStatus.DRAFT);
        request.setLogisticsNotes("Auto-generated from expiry alert: " + alert.getMessage());
        redistributionRepository.save(request);

        log.info("Auto-created NGO draft for batch {} to {}", alert.getDrugBatch().getBatchNumber(), ngo.getName());
    }
}
