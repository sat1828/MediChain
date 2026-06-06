package com.medichain.domain.repository;

import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.Enums.VEDClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DrugSKURepository extends JpaRepository<DrugSKU, UUID> {

    List<DrugSKU> findByGenericNameContainingIgnoreCase(String name);

    List<DrugSKU> findByBrandNameContainingIgnoreCase(String brand);

    @Query("SELECT d FROM DrugSKU d WHERE d.vedClassification = :ved AND d.isActive = true")
    List<DrugSKU> findByVEDClassification(@Param("ved") VEDClassification ved);

    @Query("SELECT d FROM DrugSKU d WHERE d.scheduledDrug = true AND d.isActive = true")
    List<DrugSKU> findScheduledDrugs();

    @Query("SELECT d FROM DrugSKU d WHERE d.narcotic = true AND d.isActive = true")
    List<DrugSKU> findNarcoticDrugs();

    @Query("SELECT d FROM DrugSKU d WHERE d.hsnCode = :hsnCode")
    List<DrugSKU> findByHsnCode(@Param("hsnCode") String hsnCode);
}
