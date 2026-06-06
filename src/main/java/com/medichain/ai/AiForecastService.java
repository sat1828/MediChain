package com.medichain.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.Enums.TransactionType;
import com.medichain.domain.entity.Ward;
import com.medichain.domain.repository.AiForecastReportRepository;
import com.medichain.domain.repository.DrugBatchRepository;
import com.medichain.domain.repository.DrugSKURepository;
import com.medichain.domain.repository.StockTransactionRepository;
import com.medichain.domain.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiForecastService {

    private final ClaudeApiClient claudeApiClient;
    private final ForecastPersistenceService forecastPersistence;
    private final AiForecastReportRepository forecastRepository;
    private final StockTransactionRepository transactionRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final DrugSKURepository drugSkuRepository;
    private final WardRepository wardRepository;
    private final ObjectMapper objectMapper;

    @Value("${medichain.ai.forecasting.min-transaction-days:90}")
    private int minTransactionDays;

    @Value("${medichain.ai.forecasting.min-forecast-interval-hours:24}")
    private int minForecastIntervalHours;

    @Value("${medichain.ai.claude.model}")
    private String modelVersion;

    private static final String SYSTEM_PROMPT = """
        You are MediChain's pharmacy intelligence engine. You analyze drug consumption patterns 
        in hospital wards and generate procurement forecasts. You always return valid JSON matching 
        the AiForecastResponse schema. You understand Indian hospital seasonal patterns, 
        FEFO inventory logic, and ABC-VED drug criticality classifications.
        
        Respond ONLY with valid JSON matching this schema:
        {
          "predictedStockoutDate": "YYYY-MM-DD",
          "recommendedOrderQuantity": integer,
          "forecastConfidenceScore": 0.0-1.0,
          "keyRiskFactors": ["string", ...],
          "suggestedTransferOpportunities": [{"fromWardId": "uuid", "fromWardName": "string", "batchId": "uuid", "batchNumber": "string", "quantity": integer}]
        }
        """;

    public AiForecastReport generateForecast(UUID wardId, UUID skuId) {
        var context = loadContext(wardId, skuId);
        if (context == null) return null;

        var startTime = System.currentTimeMillis();
        var claudeResponse = claudeApiClient.sendMessage(SYSTEM_PROMPT, context.userPrompt());

        var responseText = claudeResponse.content().stream()
            .filter(c -> "text".equals(c.type()))
            .map(ClaudeApiClient.ClaudeResponse.ContentBlock::text)
            .collect(Collectors.joining());

        if (claudeResponse.usage() != null) {
            log.info("Claude API usage - input: {} tokens, output: {} tokens, model: {}",
                claudeResponse.usage().inputTokens(), claudeResponse.usage().outputTokens(), modelVersion);
        }

        return forecastPersistence.save(context.ward(), context.drugSku(), responseText, startTime, modelVersion);
    }

    private ForecastContext loadContext(UUID wardId, UUID skuId) {
        var ward = wardRepository.findById(wardId)
            .orElseThrow(() -> new IllegalArgumentException("Ward not found: " + wardId));
        var drugSku = drugSkuRepository.findById(skuId)
            .orElseThrow(() -> new IllegalArgumentException("Drug SKU not found: " + skuId));

        var since = LocalDateTime.now().minusDays(minTransactionDays);
        var recentForecastCount = forecastRepository.countForecastsSince(wardId, skuId, since);
        if (recentForecastCount > 0) {
            log.warn("Forecast already generated for {} in {} within the last {} hours. Skipping.",
                drugSku.getGenericName(), ward.getName(), minForecastIntervalHours);
            return null;
        }

        var userPrompt = buildUserPrompt(ward, drugSku);
        return new ForecastContext(ward, drugSku, userPrompt);
    }

    private record ForecastContext(Ward ward, DrugSKU drugSku, String userPrompt) {}

    private String buildUserPrompt(Ward ward, DrugSKU drugSku) {
        var now = LocalDate.now();
        var ninetyDaysAgo = LocalDateTime.now().minusDays(90);

        var currentBatches = drugBatchRepository.findBatchesByWardAndSku(ward.getId(), drugSku.getId());
        var consumptionData = transactionRepository.getDailyConsumption(
            ward.getId(), drugSku.getId(), TransactionType.DISPENSE, ninetyDaysAgo);

        var inventoryLines = currentBatches.stream()
            .map(b -> String.format("- Batch %s: %d units, Expiry: %s",
                b.getBatchNumber(), b.getQuantityOnHand(), b.getExpiryDate()))
            .collect(Collectors.joining("\n"));

        var consumptionHistory = consumptionData.stream()
            .map(row -> row[1].toString())
            .collect(Collectors.joining(", "));

        var hospitalName = ward.getHospital() != null ? ward.getHospital().getName() : "Unknown Hospital";

        return String.format("""
            Ward: %s | Drug: %s %s | Hospital: %s, %s
            
            Current Inventory:
            %s
            
            90-Day Consumption History (daily units):
            [%s]
            
            Pending Procurement: None
            Drug Criticality: %s
            Storage Condition: %s
            Manufacturer: %s
            
            Return JSON matching the AiForecastResponse schema.
            """,
            ward.getName(),
            drugSku.getGenericName(),
            drugSku.getStrength() != null ? drugSku.getStrength() : "",
            hospitalName,
            ward.getHospital() != null && ward.getHospital().getCity() != null ? ward.getHospital().getCity() : "",
            inventoryLines.isEmpty() ? "No current stock" : inventoryLines,
            consumptionHistory.isEmpty() ? "No consumption data in last 90 days" : consumptionHistory,
            drugSku.getVedClassification() != null ? drugSku.getVedClassification().name() : "UNCLASSIFIED",
            drugSku.getStorageCondition() != null ? drugSku.getStorageCondition().name() : "AMBIENT",
            drugSku.getManufacturer() != null ? drugSku.getManufacturer() : "Various");
    }

    @Transactional(readOnly = true)
    public List<AiForecastReport> getLatestForecasts() {
        var since = LocalDateTime.now().minusDays(7);
        return forecastRepository.findRecentForecastsWithDetails(since);
    }
}
