package com.medichain.web.controller;

import com.medichain.domain.dto.response.InventoryResponse;
import com.medichain.domain.service.InventoryService;
import com.medichain.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wards")
@RequiredArgsConstructor
public class WardController {

    private final InventoryService inventoryService;

    @GetMapping("/{wardId}/inventory")
    public ResponseEntity<InventoryResponse> getInventory(
            @PathVariable UUID wardId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(inventoryService.getWardInventory(wardId, principal));
    }
}
