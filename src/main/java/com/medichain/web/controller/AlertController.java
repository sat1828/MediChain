package com.medichain.web.controller;

import com.medichain.domain.dto.response.AlertResponse;
import com.medichain.domain.repository.StockAlertRepository;
import com.medichain.domain.service.AlertEvaluatorService;
import com.medichain.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final StockAlertRepository stockAlertRepository;
    private final AlertEvaluatorService alertEvaluatorService;

    @GetMapping("/active")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AlertResponse>> getActiveAlerts() {
        var alerts = stockAlertRepository.findActiveAlerts().stream()
            .map(a -> new AlertResponse(
                a.getId(), a.getAlertType(), a.getSeverity().name(), a.getMessage(),
                a.getWard().getId(), a.getWard().getName(),
                a.getDrugSku().getId(), a.getDrugSku().getGenericName(),
                a.getDrugBatch() != null ? a.getDrugBatch().getId() : null,
                a.getDrugBatch() != null ? a.getDrugBatch().getBatchNumber() : null,
                a.getCurrentStock(), a.getDaysUntilStockout(), a.getDaysUntilExpiry(),
                a.isAcknowledged(), a.getCreatedAt()))
            .toList();
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<Void> acknowledge(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        alertEvaluatorService.acknowledgeAlert(alertId, principal.userId());
        return ResponseEntity.ok().build();
    }
}
