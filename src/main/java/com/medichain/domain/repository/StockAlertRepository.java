package com.medichain.domain.repository;

import com.medichain.domain.entity.StockAlert;
import com.medichain.domain.entity.Enums.AlertSeverity;
import com.medichain.domain.entity.Enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, UUID> {

    @Query("SELECT a FROM StockAlert a WHERE a.isActive = true AND a.resolvedAt IS NULL " +
           "ORDER BY a.createdAt DESC")
    List<StockAlert> findActiveAlerts();

    @Query("SELECT a FROM StockAlert a WHERE a.ward.id = :wardId AND a.isActive = true " +
           "AND a.resolvedAt IS NULL ORDER BY a.createdAt DESC")
    List<StockAlert> findActiveAlertsByWard(@Param("wardId") UUID wardId);

    @Query("SELECT a FROM StockAlert a WHERE a.drugSku.id = :skuId AND a.isActive = true " +
           "AND a.resolvedAt IS NULL ORDER BY a.createdAt DESC")
    List<StockAlert> findActiveAlertsBySku(@Param("skuId") UUID skuId);

    @Query("SELECT a FROM StockAlert a WHERE a.severity = :severity AND a.isActive = true " +
           "AND a.resolvedAt IS NULL ORDER BY a.createdAt DESC")
    List<StockAlert> findActiveBySeverity(@Param("severity") AlertSeverity severity);

    @Query("SELECT COUNT(a) FROM StockAlert a WHERE a.isActive = true AND a.resolvedAt IS NULL")
    long countActiveAlerts();

    @Query("SELECT a FROM StockAlert a WHERE a.ward.id = :wardId AND a.drugSku.id = :skuId " +
           "AND a.alertType = :alertType AND a.resolvedAt IS NULL AND a.isActive = true")
    List<StockAlert> findActiveByWardSkuAndType(@Param("wardId") UUID wardId,
                                                 @Param("skuId") UUID skuId,
                                                 @Param("alertType") AlertType alertType);
}
