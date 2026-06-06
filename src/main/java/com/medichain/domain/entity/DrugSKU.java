package com.medichain.domain.entity;

import com.medichain.domain.entity.Enums.ABCClassification;
import com.medichain.domain.entity.Enums.StorageCondition;
import com.medichain.domain.entity.Enums.VEDClassification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drug_sku", schema = "medichain")
@Getter
@Setter
@NoArgsConstructor
public class DrugSKU extends BaseEntity {

    @Column(name = "generic_name", nullable = false, length = 255)
    private String genericName;

    @Column(name = "brand_name", length = 255)
    private String brandName;

    @Column(name = "form", length = 50)
    private String form;

    @Column(name = "strength", length = 50)
    private String strength;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure;

    @Column(name = "hsn_code", length = 8)
    private String hsnCode;

    @Column(name = "scheduled_drug", nullable = false)
    private boolean scheduledDrug;

    @Column(name = "narcotic", nullable = false)
    private boolean narcotic;

    @Enumerated(EnumType.STRING)
    @Column(name = "ved_classification", length = 20)
    private VEDClassification vedClassification;

    @Enumerated(EnumType.STRING)
    @Column(name = "abc_classification", length = 5)
    private ABCClassification abcClassification;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_condition", length = 20)
    private StorageCondition storageCondition;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @Column(name = "reorder_quantity")
    private Integer reorderQuantity;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "min_stock_level")
    private Integer minStockLevel;

    @Column(name = "max_stock_level")
    private Integer maxStockLevel;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private java.math.BigDecimal gstRate;

    @OneToMany(mappedBy = "drugSku")
    private List<DrugBatch> batches = new ArrayList<>();

    @OneToMany(mappedBy = "drugSku")
    private List<AiForecastReport> forecasts = new ArrayList<>();
}
