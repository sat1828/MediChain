package com.medichain.domain.repository;

import com.medichain.domain.entity.StockTransaction;
import com.medichain.domain.entity.Enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID> {

    @Query("SELECT t FROM StockTransaction t WHERE t.ward.id = :wardId " +
           "AND t.transactionTimestamp >= :since ORDER BY t.transactionTimestamp DESC")
    List<StockTransaction> findTransactionsByWardSince(@Param("wardId") UUID wardId,
                                                        @Param("since") LocalDateTime since);

    @Query("SELECT t FROM StockTransaction t WHERE t.drugBatch.drugSku.id = :skuId " +
           "AND t.ward.id = :wardId AND t.transactionType = :type " +
           "AND t.transactionTimestamp >= :since ORDER BY t.transactionTimestamp ASC")
    List<StockTransaction> findConsumptionByWardAndSku(
            @Param("wardId") UUID wardId,
            @Param("skuId") UUID skuId,
            @Param("type") TransactionType type,
            @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM StockTransaction t " +
           "WHERE t.drugBatch.drugSku.id = :skuId AND t.ward.id = :wardId " +
           "AND t.transactionType = :type AND t.transactionTimestamp >= :since")
    Integer getTotalConsumptionSince(@Param("wardId") UUID wardId,
                                      @Param("skuId") UUID skuId,
                                      @Param("type") TransactionType type,
                                      @Param("since") LocalDateTime since);

    @Query("SELECT t FROM StockTransaction t WHERE t.drugBatch.id = :batchId " +
           "ORDER BY t.transactionTimestamp DESC")
    List<StockTransaction> findTransactionsByBatch(@Param("batchId") UUID batchId);

    @Query("SELECT t FROM StockTransaction t WHERE t.performedBy.id = :userId " +
           "ORDER BY t.transactionTimestamp DESC")
    List<StockTransaction> findTransactionsByUser(@Param("userId") UUID userId);

    @Query("SELECT FUNCTION('DATE', t.transactionTimestamp), SUM(t.quantity) " +
           "FROM StockTransaction t WHERE t.drugBatch.drugSku.id = :skuId " +
           "AND t.ward.id = :wardId AND t.transactionType = :type " +
           "AND t.transactionTimestamp >= :since GROUP BY FUNCTION('DATE', t.transactionTimestamp) " +
           "ORDER BY FUNCTION('DATE', t.transactionTimestamp) ASC")
    List<Object[]> getDailyConsumption(@Param("wardId") UUID wardId,
                                       @Param("skuId") UUID skuId,
                                       @Param("type") TransactionType type,
                                       @Param("since") LocalDateTime since);
}
