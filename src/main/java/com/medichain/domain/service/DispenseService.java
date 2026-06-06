package com.medichain.domain.service;

import com.medichain.domain.dto.request.DispenseRequest;
import com.medichain.domain.dto.response.TransactionResponse;
import com.medichain.domain.entity.DrugBatch;
import com.medichain.domain.entity.Enums.TransactionType;
import com.medichain.domain.entity.StockTransaction;
import com.medichain.domain.entity.User;
import com.medichain.domain.entity.Ward;
import com.medichain.domain.repository.DrugBatchRepository;
import com.medichain.domain.repository.DrugSKURepository;
import com.medichain.domain.repository.DrugShelfRepository;
import com.medichain.domain.repository.StockTransactionRepository;
import com.medichain.domain.repository.WardRepository;
import com.medichain.security.JwtPrincipal;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispenseService {

    private final EntityManager entityManager;
    private final DrugBatchRepository drugBatchRepository;
    private final DrugSKURepository drugSkuRepository;
    private final StockTransactionRepository transactionRepository;
    private final WardRepository wardRepository;
    private final DrugShelfRepository drugShelfRepository;
    private final AlertEvaluatorService alertEvaluator;

    @Transactional
    public List<TransactionResponse> dispense(DispenseRequest request, JwtPrincipal principal) {
        var ward = wardRepository.findById(request.wardId())
            .orElseThrow(() -> new IllegalArgumentException("Ward not found: " + request.wardId()));
        var drugSku = drugSkuRepository.findById(request.drugSkuId())
            .orElseThrow(() -> new IllegalArgumentException("Drug SKU not found: " + request.drugSkuId()));

        validateWardAccess(ward, principal);

        var shelves = drugShelfRepository.findByWardId(ward.getId());
        if (shelves.isEmpty()) {
            throw new IllegalStateException("No shelves configured in ward: " + ward.getName());
        }

        var remaining = request.quantity();
        var transactions = new ArrayList<TransactionResponse>();
        var user = entityManager.getReference(User.class, principal.userId());

        var currentDate = LocalDate.now();

        for (var shelf : shelves) {
            if (remaining <= 0) break;

            var fefoBatches = drugBatchRepository.findFefoCandidates(
                drugSku.getId(), shelf.getId(), currentDate);

            for (var batch : fefoBatches) {
                if (remaining <= 0) break;
                if (!batch.hasStock() || batch.isExpired()) continue;

                var dispenseQty = Math.min(remaining, batch.getQuantityOnHand());
                var beforeQty = batch.getQuantityOnHand();
                batch.setQuantityOnHand(batch.getQuantityOnHand() - dispenseQty);
                drugBatchRepository.save(batch);

                var transaction = new StockTransaction();
                transaction.setTransactionType(TransactionType.DISPENSE);
                transaction.setDrugBatch(batch);
                transaction.setWard(ward);
                transaction.setQuantity(dispenseQty);
                transaction.setQuantityBefore(beforeQty);
                transaction.setQuantityAfter(beforeQty - dispenseQty);
                transaction.setPerformedBy(user);
                transaction.setTransactionTimestamp(LocalDateTime.now());
                transaction.setNotes(request.notes());
                transaction = transactionRepository.save(transaction);

                transactions.add(toResponse(transaction));
                remaining -= dispenseQty;

                alertEvaluator.evaluateAndAlert(ward.getId(), drugSku.getId(), batch.getId());
            }
        }

        if (remaining > 0) {
            log.warn("Partial dispense: requested {} but only {} dispensed for drug {} in ward {}",
                request.quantity(), request.quantity() - remaining, drugSku.getGenericName(), ward.getName());
        }

        return transactions;
    }

    private void validateWardAccess(Ward ward, JwtPrincipal principal) {
        if (!"PHARMACY_MANAGER".equals(principal.role()) && !"ADMIN".equals(principal.role())) {
            if (!ward.getId().equals(principal.wardId())) {
                throw new SecurityException("Access denied: cannot dispense in ward " + ward.getId());
            }
        }
    }

    private TransactionResponse toResponse(StockTransaction txn) {
        var batch = txn.getDrugBatch();
        return new TransactionResponse(
            txn.getId(), txn.getTransactionType().name(),
            batch.getId(), batch.getBatchNumber(),
            batch.getDrugSku().getGenericName(),
            txn.getWard().getId(), txn.getWard().getName(),
            txn.getQuantity(), txn.getQuantityBefore(), txn.getQuantityAfter(),
            txn.getPerformedBy().getFullName(),
            txn.getTransactionTimestamp(), txn.getReferenceNumber(), txn.getNotes());
    }
}
