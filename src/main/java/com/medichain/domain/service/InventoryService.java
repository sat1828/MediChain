package com.medichain.domain.service;

import com.medichain.domain.dto.request.ReceiptRequest;
import com.medichain.domain.dto.response.InventoryResponse;
import com.medichain.domain.dto.response.TransactionResponse;
import com.medichain.domain.entity.DrugBatch;
import com.medichain.domain.entity.DrugShelf;
import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.Enums.TransactionType;
import com.medichain.domain.entity.StockTransaction;
import com.medichain.domain.entity.User;
import com.medichain.domain.repository.DrugBatchRepository;
import com.medichain.domain.repository.DrugSKURepository;
import com.medichain.domain.repository.DrugShelfRepository;
import com.medichain.domain.repository.StockTransactionRepository;
import com.medichain.domain.repository.WardRepository;
import com.medichain.security.JwtPrincipal;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final EntityManager entityManager;
    private final WardRepository wardRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final DrugSKURepository drugSkuRepository;
    private final DrugShelfRepository drugShelfRepository;
    private final StockTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public InventoryResponse getWardInventory(UUID wardId, JwtPrincipal principal) {
        var ward = wardRepository.findById(wardId)
            .orElseThrow(() -> new IllegalArgumentException("Ward not found"));

        if (!"PHARMACY_MANAGER".equals(principal.role()) && !"ADMIN".equals(principal.role())) {
            if (!ward.getId().equals(principal.wardId())) {
                throw new SecurityException("Access denied");
            }
        }

        var batches = drugBatchRepository.findActiveBatchesByWard(wardId);
        var now = LocalDate.now();

        var drugMap = batches.stream()
            .collect(Collectors.groupingBy(b -> b.getDrugSku().getId()));

        var drugs = new ArrayList<InventoryResponse.DrugStock>();

        for (var entry : drugMap.entrySet()) {
            var skuBatches = entry.getValue();
            var sku = skuBatches.getFirst().getDrugSku();
            var totalQty = skuBatches.stream()
                .filter(b -> !b.isExpired())
                .mapToInt(DrugBatch::getQuantityOnHand)
                .sum();

            var batchDetails = skuBatches.stream()
                .map(b -> {
                    var daysToExpiry = (int) ChronoUnit.DAYS.between(now, b.getExpiryDate());
                    var expiryStatus = daysToExpiry < 30 ? "CRITICAL"
                        : daysToExpiry < 60 ? "WARNING"
                        : daysToExpiry < 90 ? "WATCH" : "OK";
                    return new InventoryResponse.BatchDetail(
                        b.getId(), b.getBatchNumber(),
                        b.getShelf().getShelfCode(), b.getQuantityOnHand(),
                        b.getExpiryDate(), daysToExpiry, expiryStatus,
                        b.getUnitCost(), b.getMrp());
                })
                .toList();

            drugs.add(new InventoryResponse.DrugStock(
                sku.getId(), sku.getGenericName(), sku.getBrandName(),
                sku.getStrength(), sku.getForm(), totalQty,
                skuBatches.size(),
                sku.getVedClassification() != null ? sku.getVedClassification().name() : null,
                sku.getAbcClassification() != null ? sku.getAbcClassification().name() : null,
                batchDetails));
        }

        return new InventoryResponse(wardId, ward.getName(), drugs);
    }

    @Transactional
    public TransactionResponse receiveStock(ReceiptRequest request, JwtPrincipal principal) {
        var ward = wardRepository.findById(request.wardId())
            .orElseThrow(() -> new IllegalArgumentException("Ward not found"));
        var drugSku = drugSkuRepository.findById(request.drugSkuId())
            .orElseThrow(() -> new IllegalArgumentException("Drug SKU not found"));
        var shelf = drugShelfRepository.findById(request.shelfId())
            .orElseThrow(() -> new IllegalArgumentException("Shelf not found"));

        if (!shelf.getWard().getId().equals(ward.getId())) {
            throw new IllegalArgumentException("Shelf does not belong to specified ward");
        }

        var batch = new DrugBatch();
        batch.setBatchNumber(request.batchNumber());
        batch.setDrugSku(drugSku);
        batch.setShelf(shelf);
        batch.setManufactureDate(request.manufactureDate());
        batch.setExpiryDate(request.expiryDate());
        batch.setQuantityOnHand(request.quantity());
        batch.setUnitCost(request.unitCost());
        batch.setMrp(request.mrp());
        drugBatchRepository.save(batch);

        var user = entityManager.getReference(User.class, principal.userId());

        var transaction = new StockTransaction();
        transaction.setTransactionType(TransactionType.RECEIPT);
        transaction.setDrugBatch(batch);
        transaction.setWard(ward);
        transaction.setQuantity(request.quantity());
        transaction.setQuantityBefore(0);
        transaction.setQuantityAfter(request.quantity());
        transaction.setPerformedBy(user);
        transaction.setTransactionTimestamp(LocalDateTime.now());
        transaction.setNotes(request.notes());
        transaction = transactionRepository.save(transaction);

        return toResponse(transaction);
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
