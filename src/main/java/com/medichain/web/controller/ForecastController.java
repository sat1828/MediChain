package com.medichain.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medichain.ai.AiForecastService;
import com.medichain.domain.dto.response.ForecastResponse;
import com.medichain.domain.entity.AiForecastReport;
import com.medichain.domain.repository.AiForecastReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forecast")
@RequiredArgsConstructor
@Slf4j
public class ForecastController {

    private final AiForecastService forecastService;
    private final AiForecastReportRepository forecastRepository;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    @GetMapping("/{wardId}/{skuId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ForecastResponse> getLatestForecast(
            @PathVariable UUID wardId, @PathVariable UUID skuId) {
        var forecasts = forecastRepository.findByWardAndSku(wardId, skuId);
        if (forecasts.isEmpty()) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(toResponse(forecasts.getFirst()));
    }

    @PostMapping("/{wardId}/{skuId}/generate")
    @Transactional
    public ResponseEntity<ForecastResponse> generateForecast(
            @PathVariable UUID wardId, @PathVariable UUID skuId) {
        var forecast = forecastService.generateForecast(wardId, skuId);
        if (forecast == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(toResponse(forecast));
    }

    @GetMapping("/latest")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ForecastResponse>> getLatestForecasts() {
        var forecasts = forecastService.getLatestForecasts();
        return ResponseEntity.ok(forecasts.stream().map(this::toResponse).toList());
    }

    private ForecastResponse toResponse(AiForecastReport f) {
        var daysToStockout = f.getPredictedStockoutDate() != null
            ? (int) ChronoUnit.DAYS.between(LocalDate.now(), f.getPredictedStockoutDate()) : null;
        return new ForecastResponse(
            f.getId(), f.getWard().getId(), f.getWard().getName(),
            f.getDrugSku().getId(), f.getDrugSku().getGenericName(),
            f.getDrugSku().getStrength(),
            f.getDrugSku().getVedClassification() != null ? f.getDrugSku().getVedClassification().name() : null,
            f.getPredictedStockoutDate(), f.getRecommendedOrderQuantity(),
            f.getConfidenceScore(), parseJsonList(f.getKeyRiskFactors()),
            parseJsonList(f.getSuggestedTransferOpportunities()),
            f.getForecastGeneratedAt(), f.isActionable());
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON list from forecast: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
