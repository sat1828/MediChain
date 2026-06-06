package com.medichain.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medichain.domain.entity.AiForecastReport;
import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.Ward;
import com.medichain.domain.repository.AiForecastReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastPersistenceService {

    private final ObjectMapper objectMapper;
    private final AiForecastReportRepository forecastRepository;

    @Transactional
    public void saveAll(List<AiForecastReport> reports) {
        forecastRepository.saveAll(reports);
        log.info("Batch saved {} forecast reports", reports.size());
    }

    @Transactional
    public AiForecastReport save(Ward ward, DrugSKU drugSku, String responseText,
                                  long startTime, String modelVersion) {
        var forecast = parseClaudeResponse(responseText, ward, drugSku);
        forecast.setModelVersion(modelVersion);
        forecast.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        forecast = forecastRepository.save(forecast);

        log.info("Forecast for {} in {}: stockout {}, confidence {}, order {}",
            drugSku.getGenericName(), ward.getName(),
            forecast.getPredictedStockoutDate(), forecast.getConfidenceScore(),
            forecast.getRecommendedOrderQuantity());

        return forecast;
    }

    private AiForecastReport parseClaudeResponse(String json, Ward ward, DrugSKU drugSku) {
        // Same parsing logic as AiForecastService - duplicated for isolation
        var report = new AiForecastReport();
        report.setWard(ward);
        report.setDrugSku(drugSku);
        report.setForecastGeneratedAt(java.time.LocalDateTime.now());
        report.setRawPayload(json);
        report.setActionable(false);

        try {
            var sanitized = json.trim();
            if (sanitized.startsWith("```json")) {
                sanitized = sanitized.substring(7, sanitized.lastIndexOf("```")).trim();
            } else if (sanitized.startsWith("```")) {
                sanitized = sanitized.substring(3, sanitized.lastIndexOf("```")).trim();
            }

            var root = objectMapper.readTree(sanitized);

            if (root.has("predictedStockoutDate") && !root.get("predictedStockoutDate").isNull()) {
                report.setPredictedStockoutDate(java.time.LocalDate.parse(root.get("predictedStockoutDate").asText()));
            }
            if (root.has("recommendedOrderQuantity") && !root.get("recommendedOrderQuantity").isNull()) {
                report.setRecommendedOrderQuantity(root.get("recommendedOrderQuantity").asInt());
            }
            if (root.has("forecastConfidenceScore") && !root.get("forecastConfidenceScore").isNull()) {
                report.setConfidenceScore(root.get("forecastConfidenceScore").asDouble());
            }
            if (root.has("keyRiskFactors") && root.get("keyRiskFactors").isArray()) {
                report.setKeyRiskFactors(root.get("keyRiskFactors").toString());
            }
            if (root.has("suggestedTransferOpportunities") && root.get("suggestedTransferOpportunities").isArray()) {
                report.setSuggestedTransferOpportunities(root.get("suggestedTransferOpportunities").toString());
            }

            if (report.getPredictedStockoutDate() != null && report.getRecommendedOrderQuantity() != null
                && report.getRecommendedOrderQuantity() > 0) {
                report.setActionable(true);
            }
        } catch (Exception e) {
            log.error("Failed to parse Claude response: {}", e.getMessage());
            report.setKeyRiskFactors("[\"PARSE_ERROR: " + e.getMessage() + "\"]");
        }

        return report;
    }
}
