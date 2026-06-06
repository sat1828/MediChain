package com.medichain.domain.entity;

import com.medichain.domain.entity.Enums.ProcurementStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procurement_order", schema = "medichain")
@Getter
@Setter
@NoArgsConstructor
public class ProcurementOrder extends BaseEntity {

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProcurementStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_id", nullable = false)
    private User generatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private java.math.BigDecimal totalAmount;

    @Column(name = "gst_amount", precision = 14, scale = 2)
    private java.math.BigDecimal gstAmount;

    @Column(name = "grand_total", precision = 14, scale = 2)
    private java.math.BigDecimal grandTotal;

    @OneToMany(mappedBy = "procurementOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcurementLineItem> lineItems = new ArrayList<>();

    public void addLineItem(ProcurementLineItem item) {
        lineItems.add(item);
        item.setProcurementOrder(this);
    }

    public void calculateTotals() {
        this.totalAmount = lineItems.stream()
            .map(ProcurementLineItem::getLineTotal)
            .filter(java.util.Objects::nonNull)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        this.gstAmount = this.totalAmount.multiply(java.math.BigDecimal.valueOf(0.12));
        this.grandTotal = this.totalAmount.add(this.gstAmount);
    }
}
