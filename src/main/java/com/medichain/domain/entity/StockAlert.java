package com.medichain.domain.entity;

import com.medichain.domain.entity.Enums.AlertSeverity;
import com.medichain.domain.entity.Enums.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_alert", schema = "medichain")
@Getter
@Setter
@NoArgsConstructor
public class StockAlert extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_sku_id", nullable = false)
    private DrugSKU drugSku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_batch_id")
    private DrugBatch drugBatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private AlertType alertType;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "current_stock")
    private Integer currentStock;

    @Column(name = "days_until_stockout")
    private Integer daysUntilStockout;

    @Column(name = "days_until_expiry")
    private Integer daysUntilExpiry;

    @Column(name = "is_acknowledged", nullable = false)
    private boolean acknowledged;

    @Column(name = "acknowledged_by_id")
    private java.util.UUID acknowledgedById;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
