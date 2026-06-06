package com.medichain.web.controller;

import com.medichain.ai.AiForecastService;
import com.medichain.dashboard.DashboardService;
import com.medichain.domain.dto.response.AlertResponse;
import com.medichain.domain.entity.DrugBatch;
import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.Hospital;
import com.medichain.domain.entity.NGO;
import com.medichain.domain.entity.NGORedistributionRequest;
import com.medichain.domain.entity.Vendor;
import com.medichain.domain.entity.Ward;
import com.medichain.domain.repository.DrugBatchRepository;
import com.medichain.domain.repository.DrugSKURepository;
import com.medichain.domain.repository.HospitalRepository;
import com.medichain.domain.repository.NGORepository;
import com.medichain.domain.repository.StockAlertRepository;
import com.medichain.domain.repository.VendorRepository;
import com.medichain.domain.repository.WardRepository;
import com.medichain.domain.service.NgoService;
import com.medichain.domain.service.ProcurementService;
import com.medichain.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final DashboardService dashboardService;
    private final ProcurementService procurementService;
    private final NgoService ngoService;
    private final AiForecastService forecastService;
    private final WardRepository wardRepository;
    private final DrugSKURepository drugSkuRepository;
    private final HospitalRepository hospitalRepository;
    private final VendorRepository vendorRepository;
    private final DrugBatchRepository drugBatchRepository;
    private final NGORepository ngoRepository;
    private final StockAlertRepository stockAlertRepository;

    @GetMapping("/")
    public String root() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var principal = getPrincipal();
        var hospitalId = principal.hospitalId();
        var dashboard = dashboardService.getPharmacyManagerDashboard(hospitalId);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("totalSkuCount", drugSkuRepository.count());
        model.addAttribute("wardsCount", wardRepository.findByHospitalId(hospitalId).size());
        addCommonAttributes(model, principal, "dashboard");
        return "dashboard";
    }

    @GetMapping("/inventory")
    public String inventory(Model model) {
        var principal = getPrincipal();
        var hospitalId = principal.hospitalId();
        model.addAttribute("wards", wardRepository.findByHospitalId(hospitalId));
        addCommonAttributes(model, principal, "inventory");
        return "inventory";
    }

    @GetMapping("/alerts")
    @Transactional(readOnly = true)
    public String alerts(Model model) {
        var principal = getPrincipal();
        var alerts = stockAlertRepository.findActiveAlerts().stream()
            .map(a -> new AlertResponse(
                a.getId(), a.getAlertType(), a.getSeverity().name(), a.getMessage(),
                a.getWard().getId(), a.getWard().getName(),
                a.getDrugSku().getId(), a.getDrugSku().getGenericName(),
                a.getDrugBatch() != null ? a.getDrugBatch().getId() : null,
                a.getDrugBatch() != null ? a.getDrugBatch().getBatchNumber() : null,
                a.getCurrentStock(), a.getDaysUntilStockout(), a.getDaysUntilExpiry(),
                a.isAcknowledged(), a.getCreatedAt()))
            .toList();
        var alertCounts = alerts.stream()
            .collect(Collectors.groupingBy(
                AlertResponse::severity,
                Collectors.counting()));
        model.addAttribute("alerts", alerts);
        model.addAttribute("alertCounts", alertCounts);
        addCommonAttributes(model, principal, "alerts");
        return "alerts";
    }

    @GetMapping("/procurement")
    public String procurement(Model model) {
        var principal = getPrincipal();
        var orders = procurementService.listOrders();
        var vendors = vendorRepository.findAll();
        model.addAttribute("orders", orders);
        model.addAttribute("vendors", vendors);
        addCommonAttributes(model, principal, "procurement");
        return "procurement";
    }

    @GetMapping("/ngo")
    public String ngo(Model model) {
        var principal = getPrincipal();
        var hospitalId = principal.hospitalId();
        List<NGORedistributionRequest> requests = ngoService.listRequests(null);
        List<DrugBatch> batches = drugBatchRepository.findActiveBatchesByHospital(hospitalId);
        List<NGO> ngoList = ngoRepository.findAll();
        model.addAttribute("requests", requests);
        model.addAttribute("batches", batches);
        model.addAttribute("ngoList", ngoList);
        addCommonAttributes(model, principal, "ngo");
        return "ngo";
    }

    @GetMapping("/forecast")
    public String forecast(Model model) {
        var principal = getPrincipal();
        var hospitalId = principal.hospitalId();
        var wards = wardRepository.findByHospitalId(hospitalId);
        var forecasts = forecastService.getLatestForecasts();
        model.addAttribute("wards", wards);
        model.addAttribute("forecasts", forecasts);
        addCommonAttributes(model, principal, "forecast");
        return "forecast";
    }

    private JwtPrincipal getPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtPrincipal p) {
            return p;
        }
        throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Authentication required");
    }

    @Transactional(readOnly = true)
    public void addCommonAttributes(Model model, JwtPrincipal principal, String activePage) {
        model.addAttribute("hospitalId", principal.hospitalId());
        model.addAttribute("userId", principal.userId());
        model.addAttribute("userName", principal.username());
        model.addAttribute("userRole", principal.role());
        var hospital = hospitalRepository.findById(principal.hospitalId()).orElse(null);
        model.addAttribute("hospitalName", hospital != null ? hospital.getName() : "Hospital");
        model.addAttribute("activePage", activePage);
    }
}
