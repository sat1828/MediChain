package com.medichain.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "drug_batch", schema = "medichain",
       indexes = {
           @Index(name = "idx_batch_expiry", columnList = "expiry_date"),
           @Index(name = "idx_batch_sku_ward", columnList = "drug_sku_id, shelf_id"),
           @Index(name = "idx_batch_expiry_active", columnList = "expiry_date, quantity_on_hand")
       })
@Getter
@Setter
@NoArgsConstructor
public class DrugBatch extends BaseEntity {

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_sku_id", nullable = false)
    private DrugSKU drugSku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_id", nullable = false)
    private DrugShelf shelf;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand;

    @Column(name = "unit_cost", precision = 12, scale = 2)
    private java.math.BigDecimal unitCost;

    @Column(name = "mrp", precision = 12, scale = 2)
    private java.math.BigDecimal mrp;

    @Column(name = "batch_notes", length = 1000)
    private String batchNotes;

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public boolean isExpiringWithin(int days) {
        return expiryDate != null && !expiryDate.isBefore(LocalDate.now())
               && expiryDate.isBefore(LocalDate.now().plusDays(days));
    }

    public boolean hasStock() {
        return quantityOnHand != null && quantityOnHand > 0;
    }
}
