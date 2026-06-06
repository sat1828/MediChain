package com.medichain.domain.repository;

import com.medichain.domain.entity.AiForecastReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiForecastReportRepository extends JpaRepository<AiForecastReport, UUID> {

    @Query("SELECT f FROM AiForecastReport f WHERE f.ward.id = :wardId AND f.drugSku.id = :skuId " +
           "ORDER BY f.forecastGeneratedAt DESC")
    List<AiForecastReport> findByWardAndSku(@Param("wardId") UUID wardId, @Param("skuId") UUID skuId);

    @Query("SELECT f FROM AiForecastReport f WHERE f.ward.id = :wardId AND f.drugSku.id = :skuId " +
           "AND f.forecastGeneratedAt >= :since ORDER BY f.forecastGeneratedAt DESC")
    Optional<AiForecastReport> findLatestByWardAndSkuSince(
            @Param("wardId") UUID wardId,
            @Param("skuId") UUID skuId,
            @Param("since") LocalDateTime since);

    @Query("SELECT f FROM AiForecastReport f WHERE f.isActionable = true AND f.isActive = true " +
           "ORDER BY f.confidenceScore DESC")
    List<AiForecastReport> findActionableForecasts();

    @Query("SELECT f FROM AiForecastReport f JOIN FETCH f.ward JOIN FETCH f.drugSku " +
           "WHERE f.forecastGeneratedAt >= :since ORDER BY f.predictedStockoutDate ASC")
    List<AiForecastReport> findRecentForecastsWithDetails(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(f) FROM AiForecastReport f WHERE f.ward.id = :wardId AND f.drugSku.id = :skuId " +
           "AND f.forecastGeneratedAt >= :since")
    long countForecastsSince(@Param("wardId") UUID wardId, @Param("skuId") UUID skuId,
                              @Param("since") LocalDateTime since);
}
