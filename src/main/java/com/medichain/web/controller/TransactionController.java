package com.medichain.web.controller;

import com.medichain.domain.dto.request.DispenseRequest;
import com.medichain.domain.dto.request.ReceiptRequest;
import com.medichain.domain.dto.request.TransferRequest;
import com.medichain.domain.dto.response.TransactionResponse;
import com.medichain.domain.service.DispenseService;
import com.medichain.domain.service.InventoryService;
import com.medichain.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final DispenseService dispenseService;
    private final InventoryService inventoryService;

    @PostMapping("/dispense")
    public ResponseEntity<List<TransactionResponse>> dispense(
            @Valid @RequestBody DispenseRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        var result = dispenseService.dispense(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/receipt")
    public ResponseEntity<TransactionResponse> receipt(
            @Valid @RequestBody ReceiptRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(inventoryService.receiveStock(request, principal));
    }
}
