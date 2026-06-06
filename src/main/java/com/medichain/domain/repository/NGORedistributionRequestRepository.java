package com.medichain.domain.repository;

import com.medichain.domain.entity.Enums.NGOTransferStatus;
import com.medichain.domain.entity.NGORedistributionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NGORedistributionRequestRepository extends JpaRepository<NGORedistributionRequest, UUID> {

    @Query("SELECT r FROM NGORedistributionRequest r WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<NGORedistributionRequest> findByStatus(@Param("status") NGOTransferStatus status);

    @Query("SELECT r FROM NGORedistributionRequest r WHERE r.requestingNgo.id = :ngoId " +
           "ORDER BY r.createdAt DESC")
    List<NGORedistributionRequest> findByNgoId(@Param("ngoId") UUID ngoId);

    @Query("SELECT r FROM NGORedistributionRequest r WHERE r.drugBatch.id = :batchId " +
           "ORDER BY r.createdAt DESC")
    List<NGORedistributionRequest> findByDrugBatchId(@Param("batchId") UUID batchId);
}
