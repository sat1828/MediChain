package com.medichain.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medichain.domain.entity.AiForecastReport;
import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.Enums.VEDClassification;
import com.medichain.domain.entity.Hospital;
import com.medichain.domain.entity.Ward;
import com.medichain.domain.repository.AiForecastReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AiForecastServiceTest {

    private ObjectMapper objectMapper;
    private ForecastPersistenceService persistenceService;
    private Ward ward;
    private DrugSKU drugSku;

    @Mock private AiForecastReportRepository forecastRepository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        var hospital = new Hospital();
        hospital.setId(UUID.randomUUID());
        hospital.setName("Test Hospital");
        hospital.setCity("Bhubaneswar");

        ward = new Ward();
        ward.setId(UUID.randomUUID());
        ward.setName("Pediatric ICU");
        ward.setHospital(hospital);

        drugSku = new DrugSKU();
        drugSku.setId(UUID.randomUUID());
        drugSku.setGenericName("Ceftriaxone");
        drugSku.setStrength("1g IV");
        drugSku.setVedClassification(VEDClassification.VITAL);

        persistenceService = new ForecastPersistenceService(objectMapper, forecastRepository);
    }

    @Test
    void parseClaudeResponse_shouldHandleValidJson() throws Exception {
        var json = """
            {
              "predictedStockoutDate": "2025-06-15",
              "recommendedOrderQuantity": 200,
              "forecastConfidenceScore": 0.85,
              "keyRiskFactors": ["Monsoon season admission spike expected", "Single supplier dependency"],
              "suggestedTransferOpportunities": []
            }
            """;

        var report = invokeParseMethod(json);
        assertNotNull(report);
        assertEquals(LocalDate.of(2025, 6, 15), report.getPredictedStockoutDate());
        assertEquals(200, report.getRecommendedOrderQuantity());
        assertEquals(0.85, report.getConfidenceScore(), 0.001);
        assertTrue(report.isActionable());
    }

    @Test
    void parseClaudeResponse_shouldHandleMarkdownCodeBlock() throws Exception {
        var json = """
            ```json
            {
              "predictedStockoutDate": "2025-07-01",
              "recommendedOrderQuantity": 150,
              "forecastConfidenceScore": 0.72,
              "keyRiskFactors": ["Low consumption variability"],
              "suggestedTransferOpportunities": []
            }
            ```
            """;

        var report = invokeParseMethod(json);
        assertNotNull(report);
        assertEquals(LocalDate.of(2025, 7, 1), report.getPredictedStockoutDate());
        assertEquals(150, report.getRecommendedOrderQuantity());
    }

    @Test
    void parseClaudeResponse_shouldHandleMalformedJson() throws Exception {
        var json = "This is not JSON at all";

        var report = invokeParseMethod(json);
        assertNotNull(report);
        assertFalse(report.isActionable());
        assertTrue(report.getKeyRiskFactors().contains("PARSE_ERROR"));
    }

    @Test
    void parseClaudeResponse_shouldHandleNullValues() throws Exception {
        var json = """
            {
              "predictedStockoutDate": null,
              "recommendedOrderQuantity": null,
              "forecastConfidenceScore": null,
              "keyRiskFactors": [],
              "suggestedTransferOpportunities": []
            }
            """;

        var report = invokeParseMethod(json);
        assertNotNull(report);
        assertNull(report.getPredictedStockoutDate());
        assertNull(report.getRecommendedOrderQuantity());
        assertFalse(report.isActionable());
    }

    private AiForecastReport invokeParseMethod(String json) throws Exception {
        var method = ForecastPersistenceService.class.getDeclaredMethod("parseClaudeResponse",
            String.class, Ward.class, DrugSKU.class);
        method.setAccessible(true);
        return (AiForecastReport) method.invoke(persistenceService, json, ward, drugSku);
    }
}
