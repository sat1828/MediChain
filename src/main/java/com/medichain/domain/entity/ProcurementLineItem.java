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

import java.math.BigDecimal;

@Entity
@Table(name = "procurement_line_item", schema = "medichain")
@Getter
@Setter
@NoArgsConstructor
public class ProcurementLineItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procurement_order_id", nullable = false)
    private ProcurementOrder procurementOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_sku_id", nullable = false)
    private DrugSKU drugSku;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "approved_quantity")
    private Integer approvedQuantity;

    @Column(name = "received_quantity")
    private Integer receivedQuantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "justification", length = 1000)
    private String justification;

    @Column(name = "line_number")
    private Integer lineNumber;
}
