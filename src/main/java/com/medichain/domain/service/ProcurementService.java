package com.medichain.domain.service;

import com.medichain.domain.dto.request.ProcurementRequest;
import com.medichain.domain.dto.response.ProcurementOrderResponse;
import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.Enums.ProcurementStatus;
import com.medichain.domain.entity.ProcurementLineItem;
import com.medichain.domain.entity.ProcurementOrder;
import com.medichain.domain.entity.User;
import com.medichain.domain.entity.Vendor;
import com.medichain.domain.repository.DrugSKURepository;
import com.medichain.domain.repository.ProcurementOrderRepository;
import com.medichain.domain.repository.VendorRepository;
import com.medichain.security.JwtPrincipal;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcurementService {

    private final EntityManager entityManager;
    private final ProcurementOrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final DrugSKURepository drugSkuRepository;

    @Transactional
    public ProcurementOrderResponse generateOrder(ProcurementRequest request, JwtPrincipal principal) {
        var vendor = vendorRepository.findById(request.vendorId())
            .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));

        var order = new ProcurementOrder();
        order.setOrderNumber(generateOrderNumber(vendor));
        order.setStatus(ProcurementStatus.DRAFT);
        order.setVendor(vendor);
        order.setOrderDate(LocalDate.now());
        order.setNotes(request.notes());

        var generatedBy = entityManager.getReference(User.class, principal.userId());
        order.setGeneratedBy(generatedBy);

        for (var item : request.lineItems()) {
            var drugSku = drugSkuRepository.findById(item.drugSkuId())
                .orElseThrow(() -> new IllegalArgumentException("Drug SKU not found: " + item.drugSkuId()));

            var lineItem = new ProcurementLineItem();
            lineItem.setDrugSku(drugSku);
            lineItem.setRequestedQuantity(item.requestedQuantity());
            lineItem.setUnitPrice(item.unitPrice());
            lineItem.setLineTotal(item.unitPrice().multiply(BigDecimal.valueOf(item.requestedQuantity())));
            lineItem.setJustification(item.justification());
            order.addLineItem(lineItem);
        }

        order.calculateTotals();
        order = orderRepository.save(order);
        return toResponse(order);
    }

    @Transactional
    public ProcurementOrderResponse approveOrder(UUID orderId, JwtPrincipal principal) {
        var order = orderRepository.findByIdWithLineItems(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != ProcurementStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Order must be PENDING_APPROVAL to approve");
        }

        var approvedBy = entityManager.getReference(User.class, principal.userId());
        order.setApprovedBy(approvedBy);
        order.setStatus(ProcurementStatus.APPROVED);
        order = orderRepository.save(order);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<ProcurementOrderResponse> listOrders() {
        return orderRepository.findAllOrderByDateDesc().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProcurementOrderResponse getOrder(UUID orderId) {
        var order = orderRepository.findByIdWithLineItems(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public ProcurementOrder getOrderEntity(UUID orderId) {
        return orderRepository.findByIdWithLineItems(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    private String generateOrderNumber(Vendor vendor) {
        var datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        var vendorCode = vendor.getName().substring(0, Math.min(3, vendor.getName().length())).toUpperCase();
        var seq = (int) (System.currentTimeMillis() % 10000);
        return String.format("PO-%s-%s-%04d", datePart, vendorCode, seq);
    }

    private ProcurementOrderResponse toResponse(ProcurementOrder order) {
        var lineItems = order.getLineItems().stream()
            .map(item -> new ProcurementOrderResponse.LineItemResponse(
                item.getId(), item.getDrugSku().getGenericName() + " " + item.getDrugSku().getStrength(),
                item.getDrugSku().getStrength(), item.getDrugSku().getHsnCode(),
                item.getRequestedQuantity(), item.getApprovedQuantity() != null ? item.getApprovedQuantity() : 0,
                item.getUnitPrice(), item.getLineTotal(), item.getJustification()))
            .toList();

        return new ProcurementOrderResponse(
            order.getId(), order.getOrderNumber(), order.getStatus().name(),
            order.getVendor().getName(), order.getVendor().getGstin(),
            order.getOrderDate(), order.getExpectedDeliveryDate(),
            order.getGeneratedBy().getFullName(),
            order.getApprovedBy() != null ? order.getApprovedBy().getFullName() : null,
            order.getTotalAmount(), order.getGstAmount(), order.getGrandTotal(),
            lineItems, order.getNotes(),
            "/api/v1/procurement/orders/" + order.getId() + "/pdf");
    }
}
