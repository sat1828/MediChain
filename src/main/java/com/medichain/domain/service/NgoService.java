package com.medichain.domain.service;

import com.medichain.domain.dto.request.NgoRedistributionRequest;
import com.medichain.domain.entity.DrugBatch;
import com.medichain.domain.entity.Enums.NGOTransferStatus;
import com.medichain.domain.entity.Enums.TransactionType;
import com.medichain.domain.entity.NGO;
import com.medichain.domain.entity.NGORedistributionRequest;
import com.medichain.domain.entity.StockTransaction;
import com.medichain.domain.entity.User;
import com.medichain.domain.repository.DrugBatchRepository;
import com.medichain.domain.repository.NGORepository;
import com.medichain.domain.repository.NGORedistributionRequestRepository;
import com.medichain.domain.repository.StockTransactionRepository;
import com.medichain.domain.repository.WardRepository;
import com.medichain.security.JwtPrincipal;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NgoService {

    private final EntityManager entityManager;
    private final NGORedistributionRequestRepository redistributionRepository;
    private final NGORepository ngoRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final WardRepository wardRepository;
    private final StockTransactionRepository transactionRepository;

    @Transactional
    public NGORedistributionRequest createRequest(NgoRedistributionRequest request, JwtPrincipal principal) {
        var batch = drugBatchRepository.findById(request.drugBatchId())
            .orElseThrow(() -> new IllegalArgumentException("Drug batch not found"));
        var ngo = ngoRepository.findById(request.ngoId())
            .orElseThrow(() -> new IllegalArgumentException("NGO not found"));

        if (!batch.hasStock()) {
            throw new IllegalStateException("Batch has no stock available");
        }
        if (request.quantity() > batch.getQuantityOnHand()) {
            throw new IllegalArgumentException("Requested quantity exceeds available stock");
        }
        if (!batch.isExpiringWithin(45)) {
            throw new IllegalArgumentException("Batch is not within the 45-day redistribution window");
        }

        var redistribution = new NGORedistributionRequest();
        redistribution.setDrugBatch(batch);
        redistribution.setRequestingNgo(ngo);
        redistribution.setQuantityRequested(request.quantity());
        redistribution.setStatus(NGOTransferStatus.DRAFT);
        redistribution.setLogisticsNotes(request.logisticsNotes());
        redistribution = redistributionRepository.save(redistribution);

        log.info("NGO redistribution request created: {} units of batch {} to {}",
            request.quantity(), batch.getBatchNumber(), ngo.getName());
        return redistribution;
    }

    @Transactional
    public NGORedistributionRequest approveTransfer(UUID requestId, JwtPrincipal principal) {
        var redistribution = redistributionRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Redistribution request not found"));
        var batch = redistribution.getDrugBatch();

        if (!NGOTransferStatus.DRAFT.equals(redistribution.getStatus())) {
            throw new IllegalStateException("Request is not in DRAFT status");
        }

        var quantityToTransfer = redistribution.getQuantityRequested();
        if (quantityToTransfer > batch.getQuantityOnHand()) {
            throw new IllegalStateException("Insufficient stock: have " + batch.getQuantityOnHand()
                + ", need " + quantityToTransfer);
        }

        batch.setQuantityOnHand(batch.getQuantityOnHand() - quantityToTransfer);
        drugBatchRepository.save(batch);

        var user = entityManager.getReference(User.class, principal.userId());
        redistribution.setReviewedBy(user);
        redistribution.setQuantityApproved(quantityToTransfer);
        redistribution.setStatus(NGOTransferStatus.COMPLETED);
        redistribution.setCompletedAt(LocalDateTime.now());
        redistributionRepository.save(redistribution);

        var transaction = new StockTransaction();
        transaction.setTransactionType(TransactionType.NGO_TRANSFER);
        transaction.setDrugBatch(batch);
        var ward = batch.getShelf().getWard();
        transaction.setWard(ward);
        transaction.setQuantity(quantityToTransfer);
        transaction.setQuantityBefore(batch.getQuantityOnHand() + quantityToTransfer);
        transaction.setQuantityAfter(batch.getQuantityOnHand());
        transaction.setPerformedBy(user);
        transaction.setTransactionTimestamp(LocalDateTime.now());
        transaction.setNotes("NGO transfer: " + redistribution.getRequestingNgo().getName());
        transactionRepository.save(transaction);

        log.info("NGO transfer completed: {} units of batch {} to {}",
            quantityToTransfer, batch.getBatchNumber(), redistribution.getRequestingNgo().getName());
        return redistribution;
    }

    @Transactional(readOnly = true)
    public List<NGORedistributionRequest> listRequests(String status) {
        if (status != null && !status.isBlank()) {
            return redistributionRepository.findByStatus(NGOTransferStatus.valueOf(status.toUpperCase()));
        }
        return redistributionRepository.findAll();
    }
}
