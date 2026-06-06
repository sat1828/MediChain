package com.medichain.web.controller;

import com.medichain.domain.dto.request.NgoRedistributionRequest;
import com.medichain.domain.dto.response.NgoRequestResponse;
import com.medichain.domain.entity.NGORedistributionRequest;
import com.medichain.domain.service.NgoService;
import com.medichain.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ngo")
@RequiredArgsConstructor
public class NgoController {

    private final NgoService ngoService;

    @PostMapping("/redistribution/request")
    @Transactional
    public ResponseEntity<NgoRequestResponse> createRequest(
            @Valid @RequestBody NgoRedistributionRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(toResponse(ngoService.createRequest(request, principal)));
    }

    @PostMapping("/redistribution/{requestId}/approve")
    @Transactional
    public ResponseEntity<NgoRequestResponse> approveTransfer(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(toResponse(ngoService.approveTransfer(requestId, principal)));
    }

    @GetMapping("/redistribution/requests")
    @Transactional(readOnly = true)
    public ResponseEntity<List<NgoRequestResponse>> listRequests(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ngoService.listRequests(status).stream().map(this::toResponse).toList());
    }

    private NgoRequestResponse toResponse(NGORedistributionRequest r) {
        return new NgoRequestResponse(
            r.getId(),
            r.getDrugBatch().getId(), r.getDrugBatch().getBatchNumber(),
            r.getRequestingNgo().getId(), r.getRequestingNgo().getName(),
            r.getQuantityRequested(), r.getQuantityApproved(),
            r.getStatus().name(),
            r.getReviewedBy() != null ? r.getReviewedBy().getFullName() : null,
            r.getLogisticsNotes(), r.getPickupDate(),
            r.getCreatedAt());
    }
}
