package com.medichain.domain.repository;

import com.medichain.domain.entity.ExpiryAlertRecord;
import com.medichain.domain.entity.Enums.AlertSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExpiryAlertRecordRepository extends JpaRepository<ExpiryAlertRecord, UUID> {

    @Query("SELECT e FROM ExpiryAlertRecord e WHERE e.isActive = true AND e.resolved = false " +
           "ORDER BY e.daysToExpiry ASC")
    List<ExpiryAlertRecord> findActiveExpiryAlerts();

    @Query("SELECT e FROM ExpiryAlertRecord e WHERE e.severity = :severity AND e.resolved = false " +
           "ORDER BY e.createdAt DESC")
    List<ExpiryAlertRecord> findBySeverity(@Param("severity") AlertSeverity severity);

    @Query("SELECT e FROM ExpiryAlertRecord e WHERE e.drugBatch.id = :batchId ORDER BY e.createdAt DESC")
    List<ExpiryAlertRecord> findByDrugBatchId(@Param("batchId") UUID batchId);
}
