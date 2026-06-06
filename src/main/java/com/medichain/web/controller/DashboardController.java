package com.medichain.web.controller;

import com.medichain.dashboard.DashboardService;
import com.medichain.domain.dto.response.DashboardResponse;
import com.medichain.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/pharmacy-manager")
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(dashboardService.getPharmacyManagerDashboard(principal.hospitalId()));
    }
}
