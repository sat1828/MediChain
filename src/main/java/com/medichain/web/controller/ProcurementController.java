package com.medichain.web.controller;

import com.medichain.domain.dto.request.ProcurementRequest;
import com.medichain.domain.dto.response.ProcurementOrderResponse;
import com.medichain.domain.service.ProcurementService;
import com.medichain.reporting.ProcurementOrderPdfGenerator;
import com.medichain.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;
    private final ProcurementOrderPdfGenerator pdfGenerator;

    @PostMapping("/orders/generate")
    public ResponseEntity<ProcurementOrderResponse> generateOrder(
            @Valid @RequestBody ProcurementRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(procurementService.generateOrder(request, principal));
    }

    @PostMapping("/orders/{orderId}/approve")
    public ResponseEntity<ProcurementOrderResponse> approveOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(procurementService.approveOrder(orderId, principal));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<ProcurementOrderResponse>> listOrders() {
        return ResponseEntity.ok(procurementService.listOrders());
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ProcurementOrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(procurementService.getOrder(orderId));
    }

    @GetMapping("/orders/{orderId}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID orderId) {
        var order = procurementService.getOrderEntity(orderId);
        var pdf = pdfGenerator.generatePdf(order);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=PO-" + orderId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
