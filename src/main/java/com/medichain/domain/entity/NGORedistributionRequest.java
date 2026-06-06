package com.medichain.domain.entity;

import com.medichain.domain.entity.Enums.NGOTransferStatus;
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

@Entity
@Table(name = "ngo_redistribution_request", schema = "medichain")
@Getter
@Setter
@NoArgsConstructor
public class NGORedistributionRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_batch_id", nullable = false)
    private DrugBatch drugBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_ngo_id", nullable = false)
    private NGO requestingNgo;

    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;

    @Column(name = "quantity_approved")
    private Integer quantityApproved;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NGOTransferStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "logistics_notes", length = 2000)
    private String logisticsNotes;

    @Column(name = "pickup_date")
    private java.time.LocalDate pickupDate;

    @Column(name = "donation_certificate_generated", nullable = false)
    private boolean donationCertificateGenerated;

    @Column(name = "completed_at")
    private java.time.LocalDateTime completedAt;
}
