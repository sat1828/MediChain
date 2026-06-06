package com.medichain.domain.repository;

import com.medichain.domain.entity.ProcurementOrder;
import com.medichain.domain.entity.Enums.ProcurementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcurementOrderRepository extends JpaRepository<ProcurementOrder, UUID> {

    List<ProcurementOrder> findByStatus(ProcurementStatus status);

    @Query("SELECT o FROM ProcurementOrder o LEFT JOIN FETCH o.lineItems li " +
           "LEFT JOIN FETCH li.drugSku " +
           "JOIN FETCH o.vendor JOIN FETCH o.generatedBy LEFT JOIN FETCH o.approvedBy " +
           "WHERE o.id = :orderId")
    Optional<ProcurementOrder> findByIdWithLineItems(@Param("orderId") UUID orderId);

    @Query("SELECT o FROM ProcurementOrder o WHERE o.generatedBy.id = :userId ORDER BY o.orderDate DESC")
    List<ProcurementOrder> findByGeneratedBy(@Param("userId") UUID userId);

    @Query("SELECT o FROM ProcurementOrder o LEFT JOIN FETCH o.lineItems li " +
           "LEFT JOIN FETCH li.drugSku " +
           "JOIN FETCH o.vendor JOIN FETCH o.generatedBy LEFT JOIN FETCH o.approvedBy " +
           "ORDER BY o.orderDate DESC")
    List<ProcurementOrder> findAllOrderByDateDesc();

    Optional<ProcurementOrder> findByOrderNumber(String orderNumber);

    @Query("SELECT o.status, COUNT(o) FROM ProcurementOrder o GROUP BY o.status")
    List<Object[]> countByStatus();
}
