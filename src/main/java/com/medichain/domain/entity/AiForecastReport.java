package com.medichain.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_forecast_report", schema = "medichain")
@Getter
@Setter
@NoArgsConstructor
public class AiForecastReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_sku_id", nullable = false)
    private DrugSKU drugSku;

    @Column(name = "forecast_generated_at", nullable = false)
    private LocalDateTime forecastGeneratedAt;

    @Column(name = "predicted_stockout_date")
    private LocalDate predictedStockoutDate;

    @Column(name = "recommended_order_quantity")
    private Integer recommendedOrderQuantity;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "key_risk_factors", length = 2000)
    private String keyRiskFactors;

    @Column(name = "suggested_transfers", length = 2000)
    private String suggestedTransferOpportunities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "is_actionable", nullable = false)
    private boolean actionable;
}
