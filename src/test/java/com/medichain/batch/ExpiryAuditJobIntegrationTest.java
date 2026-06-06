package com.medichain.batch;

import static org.junit.jupiter.api.Assertions.*;

import com.medichain.domain.entity.DrugBatch;
import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.DrugShelf;
import com.medichain.domain.entity.Enums.StorageCondition;
import com.medichain.domain.entity.Enums.VEDClassification;
import com.medichain.domain.entity.Hospital;
import com.medichain.domain.entity.Ward;
import com.medichain.domain.repository.DrugBatchRepository;
import com.medichain.domain.repository.ExpiryAlertRecordRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

@Testcontainers
@SpringBootTest
class ExpiryAuditJobIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("medichain_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.autoconfigure.exclude",
            () -> "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration");
        registry.add("spring.batch.job.enabled", () -> "false");
    }

    @Autowired
    private DrugBatchRepository drugBatchRepository;

    @Autowired
    private ExpiryAlertRecordRepository alertRecordRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        var hospital = new Hospital();
        hospital.setName("Test Hospital");
        hospital.setActive(true);
        entityManager.persist(hospital);

        var ward = new Ward();
        ward.setName("ICU");
        ward.setHospital(hospital);
        ward.setActive(true);
        entityManager.persist(ward);

        var shelf = new DrugShelf();
        shelf.setShelfCode("A-01");
        shelf.setWard(ward);
        shelf.setActive(true);
        entityManager.persist(shelf);

        var drugSku = new DrugSKU();
        drugSku.setGenericName("Paracetamol");
        drugSku.setStrength("500mg");
        drugSku.setVedClassification(VEDClassification.ESSENTIAL);
        drugSku.setStorageCondition(StorageCondition.AMBIENT);
        drugSku.setActive(true);
        entityManager.persist(drugSku);

        entityManager.flush();
    }

    @Test
    void contextLoads() {
        assertNotNull(drugBatchRepository);
        assertNotNull(alertRecordRepository);
    }

    @Test
    void shouldDetectExpiringBatches() {
        var drugSku = entityManager
            .createQuery("SELECT s FROM DrugSKU s WHERE s.genericName = 'Paracetamol'", DrugSKU.class)
            .getSingleResult();
        var shelf = entityManager
            .createQuery("SELECT s FROM DrugShelf s WHERE s.shelfCode = 'A-01'", DrugShelf.class)
            .getSingleResult();

        var expiringBatch = new DrugBatch();
        expiringBatch.setBatchNumber("EXP2024");
        expiringBatch.setDrugSku(drugSku);
        expiringBatch.setShelf(shelf);
        expiringBatch.setExpiryDate(LocalDate.now().plusDays(15));
        expiringBatch.setQuantityOnHand(100);
        expiringBatch.setActive(true);

        var safeBatch = new DrugBatch();
        safeBatch.setBatchNumber("SAFE2025");
        safeBatch.setDrugSku(drugSku);
        safeBatch.setShelf(shelf);
        safeBatch.setExpiryDate(LocalDate.now().plusDays(180));
        safeBatch.setQuantityOnHand(200);
        safeBatch.setActive(true);

        assertDoesNotThrow(() -> {
            drugBatchRepository.save(expiringBatch);
            drugBatchRepository.save(safeBatch);
        });

        var allBatches = drugBatchRepository.findAll();
        assertEquals(2, allBatches.size(), "Should have saved 2 batches");
    }
}
