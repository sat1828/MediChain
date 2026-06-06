package com.medichain.domain.repository;

import com.medichain.domain.entity.DrugBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DrugBatchRepository extends JpaRepository<DrugBatch, UUID> {

    @Query("SELECT b FROM DrugBatch b WHERE b.drugSku.id = :skuId AND b.shelf.id = :shelfId " +
           "AND b.quantityOnHand > 0 AND b.expiryDate >= :currentDate " +
           "ORDER BY b.expiryDate ASC")
    @Lock(LockModeType.PESSIMISTIC_READ)
    List<DrugBatch> findFefoCandidates(@Param("skuId") UUID skuId,
                                       @Param("shelfId") UUID shelfId,
                                       @Param("currentDate") LocalDate currentDate);

    @Query("SELECT b FROM DrugBatch b WHERE b.expiryDate < :cutoffDate AND b.quantityOnHand > 0")
    Page<DrugBatch> findExpiringBatches(@Param("cutoffDate") LocalDate cutoffDate, Pageable pageable);

    @Query("SELECT b FROM DrugBatch b WHERE b.expiryDate BETWEEN :startDate AND :endDate AND b.quantityOnHand > 0")
    Page<DrugBatch> findBatchesExpiringBetween(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate,
                                                Pageable pageable);

    @Query("SELECT b FROM DrugBatch b JOIN FETCH b.shelf s JOIN FETCH s.ward JOIN FETCH b.drugSku " +
           "WHERE b.expiryDate BETWEEN :startDate AND :endDate AND b.quantityOnHand > 0 " +
           "ORDER BY b.expiryDate ASC")
    List<DrugBatch> findExpiringBatchesWithDetails(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM DrugBatch b WHERE b.expiryDate < :criticalDate AND b.quantityOnHand > 0 " +
           "AND b.isActive = true ORDER BY b.expiryDate ASC")
    List<DrugBatch> findCriticalExpiryBatches(@Param("criticalDate") LocalDate criticalDate);

    @Query("SELECT b FROM DrugBatch b JOIN b.shelf s WHERE s.ward.id = :wardId " +
           "AND b.drugSku.id = :skuId AND b.quantityOnHand > 0 ORDER BY b.expiryDate ASC")
    List<DrugBatch> findBatchesByWardAndSku(@Param("wardId") UUID wardId,
                                            @Param("skuId") UUID skuId);

    @Query("SELECT COALESCE(SUM(b.quantityOnHand), 0) FROM DrugBatch b JOIN b.shelf s " +
           "WHERE s.ward.id = :wardId AND b.drugSku.id = :skuId AND b.expiryDate >= :currentDate")
    Integer getTotalStockByWardAndSku(@Param("wardId") UUID wardId,
                                       @Param("skuId") UUID skuId,
                                       @Param("currentDate") LocalDate currentDate);

    @Query("SELECT b FROM DrugBatch b JOIN FETCH b.drugSku JOIN FETCH b.shelf s JOIN FETCH s.ward " +
           "WHERE s.ward.id = :wardId AND b.quantityOnHand > 0 ORDER BY b.drugSku.genericName, b.expiryDate ASC")
    List<DrugBatch> findActiveBatchesByWard(@Param("wardId") UUID wardId);

    @Query("SELECT b FROM DrugBatch b WHERE b.batchNumber = :batchNumber AND b.isActive = true")
    Optional<DrugBatch> findByBatchNumber(@Param("batchNumber") String batchNumber);

    @Query("SELECT b FROM DrugBatch b WHERE b.shelf.id = :shelfId AND b.quantityOnHand > 0 " +
           "ORDER BY b.expiryDate ASC")
    List<DrugBatch> findActiveByShelf(@Param("shelfId") UUID shelfId);

    @Query("SELECT b FROM DrugBatch b WHERE b.drugSku.id = :skuId AND b.expiryDate >= :date " +
            "AND b.quantityOnHand > 0 ORDER BY b.expiryDate ASC")
    List<DrugBatch> findAvailableStockBySku(@Param("skuId") UUID skuId, @Param("date") LocalDate date);

    @Query("SELECT b FROM DrugBatch b JOIN FETCH b.drugSku JOIN FETCH b.shelf s JOIN FETCH s.ward w " +
            "WHERE w.hospital.id = :hospitalId AND b.quantityOnHand > 0 AND b.isActive = true " +
            "ORDER BY b.drugSku.genericName, b.expiryDate ASC")
    List<DrugBatch> findActiveBatchesByHospital(@Param("hospitalId") UUID hospitalId);
}
