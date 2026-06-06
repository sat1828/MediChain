package com.medichain.domain.entity;

import com.medichain.domain.entity.Enums.AlertSeverity;
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

import java.time.LocalDate;

@Entity
@Table(name = "expiry_alert_record", schema = "medichain")
@Getter
@Setter
@NoArgsConstructor
public class ExpiryAlertRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_batch_id", nullable = false)
    private DrugBatch drugBatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "days_to_expiry")
    private Integer daysToExpiry;

    @Column(name = "alert_message", length = 1000)
    private String alertMessage;

    @Column(name = "notified_pharmacy_manager", nullable = false)
    private boolean notifiedPharmacyManager;

    @Column(name = "notified_ward_pharmacist", nullable = false)
    private boolean notifiedWardPharmacist;

    @Column(name = "ngo_draft_created", nullable = false)
    private boolean ngoDraftCreated;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    @Column(name = "resolved_at")
    private java.time.LocalDateTime resolvedAt;
}
