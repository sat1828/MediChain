package com.medichain.config;

import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.Enums.ABCClassification;
import com.medichain.domain.entity.Enums.StorageCondition;
import com.medichain.domain.entity.Enums.UserRole;
import com.medichain.domain.entity.Enums.VEDClassification;
import com.medichain.domain.entity.Hospital;
import com.medichain.domain.entity.User;
import com.medichain.domain.entity.Ward;
import com.medichain.domain.repository.HospitalRepository;
import com.medichain.domain.repository.UserRepository;
import com.medichain.domain.repository.WardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final HospitalRepository hospitalRepository;
    private final WardRepository wardRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            log.info("Seed data already exists, skipping...");
            return;
        }

        log.info("Loading seed data...");

        var hospital = new Hospital();
        hospital.setName("MediChain Demo Hospital");
        hospital.setRegistrationNumber("MCH-DEMO-001");
        hospital.setCity("Bhubaneswar");
        hospital.setState("Odisha");
        hospital.setPincode("751001");
        hospital.setGstin("21ABCDE1234F1Z5");
        hospital.setPharmacyLicenseNumber("PL-OD-2024-001");
        hospital.setPhone("+91-674-2555000");
        hospital.setEmail("pharmacy@demohospital.in");
        hospital.setBedCount(350);
        hospital = hospitalRepository.save(hospital);
        log.info("Created hospital: {}", hospital.getName());

        var wardIcu = createWard(hospital, "ICU", "ICU-01", "Ground Floor");
        var wardOt = createWard(hospital, "Operation Theatre", "OT-01", "First Floor");
        var wardCardio = createWard(hospital, "Cardiology", "CARD-01", "Second Floor");
        var wardNephro = createWard(hospital, "Nephrology", "NEPH-01", "Second Floor");
        var wardPedia = createWard(hospital, "Pediatrics", "PED-01", "Third Floor");
        var wardGeneral = createWard(hospital, "General Medicine", "GEN-01", "Ground Floor");

        createUser("admin", "admin123", "System Administrator", UserRole.ADMIN, hospital, null);
        createUser("manager1", "manager123", "Dr. Anjali Sharma", UserRole.PHARMACY_MANAGER, hospital, null);
        createUser("icu_pharm", "pharm123", "Ravi Kumar", UserRole.WARD_PHARMACIST, hospital, wardIcu);
        createUser("ot_pharm", "pharm123", "Sneha Patel", UserRole.WARD_PHARMACIST, hospital, wardOt);
        createUser("cardio_pharm", "pharm123", "Arun Singh", UserRole.WARD_PHARMACIST, hospital, wardCardio);
        createUser("procure1", "procure123", "Vikram Mehta", UserRole.PROCUREMENT_OFFICER, hospital, null);
        createUser("ngo1", "ngo123", "Priya Das", UserRole.NGO_PARTNER, hospital, null);

        log.info("Seed data loaded successfully. Admin login: admin / admin123");
    }

    private Ward createWard(Hospital hospital, String name, String code, String floor) {
        var ward = new Ward();
        ward.setName(name);
        ward.setCode(code);
        ward.setFloor(floor);
        ward.setHospital(hospital);
        return wardRepository.save(ward);
    }

    private void createUser(String username, String password, String fullName,
                             UserRole role, Hospital hospital, Ward ward) {
        var user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setHospital(hospital);
        user.setWard(ward);
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}
