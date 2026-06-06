package com.medichain.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drug_shelf", schema = "medichain")
@Getter
@Setter
@NoArgsConstructor
public class DrugShelf extends BaseEntity {

    @Column(name = "shelf_code", nullable = false, length = 50)
    private String shelfCode;

    @Column(name = "location_description", length = 255)
    private String locationDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;

    @OneToMany(mappedBy = "shelf")
    private List<DrugBatch> batches = new ArrayList<>();
}
