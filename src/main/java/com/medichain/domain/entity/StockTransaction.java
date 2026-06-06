package com.medichain.domain.entity;

import com.medichain.domain.entity.Enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transaction", schema = "medichain",
       indexes = {
           @Index(name = "idx_txn_batch", columnList = "drug_batch_id"),
           @Index(name = "idx_txn_timestamp", columnList = "timestamp"),
           @Index(name = "idx_txn_ward_type", columnList = "ward_id, transaction_type")
       })
@Getter
@Setter
@NoArgsConstructor
public class StockTransaction extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_batch_id", nullable = false)
    private DrugBatch drugBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;

    @Column(name = "source_ward_id")
    private java.util.UUID sourceWardId;

    @Column(name = "destination_ward_id")
    private java.util.UUID destinationWardId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "quantity_before")
    private Integer quantityBefore;

    @Column(name = "quantity_after")
    private Integer quantityAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_id", nullable = false)
    private User performedBy;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "transaction_timestamp", nullable = false)
    private LocalDateTime transactionTimestamp;
}
