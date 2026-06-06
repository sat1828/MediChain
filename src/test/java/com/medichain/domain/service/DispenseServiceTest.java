package com.medichain.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.medichain.domain.dto.request.DispenseRequest;
import com.medichain.domain.entity.DrugBatch;
import com.medichain.domain.entity.DrugSKU;
import com.medichain.domain.entity.DrugShelf;
import com.medichain.domain.entity.Hospital;
import com.medichain.domain.entity.User;
import com.medichain.domain.entity.Ward;
import com.medichain.domain.repository.DrugBatchRepository;
import com.medichain.domain.repository.DrugSKURepository;
import com.medichain.domain.repository.DrugShelfRepository;
import com.medichain.domain.repository.StockTransactionRepository;
import com.medichain.domain.repository.WardRepository;
import com.medichain.security.JwtPrincipal;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class DispenseServiceTest {

    @Mock private DrugBatchRepository drugBatchRepository;
    @Mock private DrugSKURepository drugSkuRepository;
    @Mock private StockTransactionRepository transactionRepository;
    @Mock private WardRepository wardRepository;
    @Mock private DrugShelfRepository drugShelfRepository;
    @Mock private AlertEvaluatorService alertEvaluator;
    @Mock private EntityManager entityManager;

    private DispenseService dispenseService;
    private Ward ward;
    private DrugSKU drugSku;
    private DrugShelf shelf;
    private JwtPrincipal principal;

    @BeforeEach
    void setUp() {
        dispenseService = new DispenseService(entityManager, drugBatchRepository, drugSkuRepository,
            transactionRepository, wardRepository, drugShelfRepository, alertEvaluator);

        var mockUser = mock(User.class);
        when(mockUser.getFullName()).thenReturn("Test Pharmacist");
        when(entityManager.getReference(eq(User.class), any(UUID.class))).thenReturn(mockUser);

        var hospital = new Hospital();
        hospital.setId(UUID.randomUUID());

        ward = new Ward();
        ward.setId(UUID.randomUUID());
        ward.setName("ICU");
        ward.setHospital(hospital);

        shelf = new DrugShelf();
        shelf.setId(UUID.randomUUID());
        shelf.setShelfCode("A-01");
        shelf.setWard(ward);

        drugSku = new DrugSKU();
        drugSku.setId(UUID.randomUUID());
        drugSku.setGenericName("Ceftriaxone 1g");

        principal = new JwtPrincipal(UUID.randomUUID(), "pharmacist1",
            "WARD_PHARMACIST", hospital.getId(), ward.getId());
    }

    @Test
    void dispense_shouldSelectEarliestExpiryBatchFirst_FEFO() {
        var request = new DispenseRequest(ward.getId(), drugSku.getId(), 50, "Routine dispense");

        var batch1 = createBatch("B001", LocalDate.now().plusDays(30), 40);
        var batch2 = createBatch("B002", LocalDate.now().plusDays(60), 40);
        var batch3 = createBatch("B003", LocalDate.now().plusDays(15), 40);

        when(wardRepository.findById(ward.getId())).thenReturn(Optional.of(ward));
        when(drugSkuRepository.findById(drugSku.getId())).thenReturn(Optional.of(drugSku));
        when(drugShelfRepository.findByWardId(ward.getId())).thenReturn(List.of(shelf));
        when(drugBatchRepository.findFefoCandidates(drugSku.getId(), shelf.getId(), LocalDate.now()))
            .thenReturn(List.of(batch3, batch1, batch2));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        dispenseService.dispense(request, principal);

        var batchCaptor = ArgumentCaptor.forClass(DrugBatch.class);
        verify(drugBatchRepository, times(2)).save(batchCaptor.capture());
        var savedBatches = batchCaptor.getAllValues();

        assertEquals(0, savedBatches.get(0).getQuantityOnHand(),
            "Batch expiring soonest should be fully consumed first");
        assertEquals(30, savedBatches.get(1).getQuantityOnHand(),
            "10 units should be taken from batch expiring next");
    }

    @Test
    void dispense_shouldDispenseFromSingleBatchWhenSufficient() {
        var request = new DispenseRequest(ward.getId(), drugSku.getId(), 10, "Small dispense");

        var batch = createBatch("B010", LocalDate.now().plusDays(90), 50);

        when(wardRepository.findById(ward.getId())).thenReturn(Optional.of(ward));
        when(drugSkuRepository.findById(drugSku.getId())).thenReturn(Optional.of(drugSku));
        when(drugShelfRepository.findByWardId(ward.getId())).thenReturn(List.of(shelf));
        when(drugBatchRepository.findFefoCandidates(drugSku.getId(), shelf.getId(), LocalDate.now()))
            .thenReturn(List.of(batch));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var transactions = dispenseService.dispense(request, principal);

        assertFalse(transactions.isEmpty());
        assertEquals(40, batch.getQuantityOnHand());
    }

    @Test
    void dispense_shouldRejectCrossWardAccessForNonManager() {
        var otherWard = new Ward();
        otherWard.setId(UUID.randomUUID());

        when(wardRepository.findById(otherWard.getId())).thenReturn(Optional.of(otherWard));
        when(drugSkuRepository.findById(drugSku.getId())).thenReturn(Optional.of(drugSku));

        var request = new DispenseRequest(otherWard.getId(), drugSku.getId(), 10, "Test");
        assertThrows(SecurityException.class, () -> dispenseService.dispense(request, principal));
    }

    @Test
    void dispense_shouldAllowCrossWardAccessForManager() {
        var otherWard = new Ward();
        otherWard.setId(UUID.randomUUID());

        var managerPrincipal = new JwtPrincipal(UUID.randomUUID(), "manager1",
            "PHARMACY_MANAGER", UUID.randomUUID(), null);

        var shelf2 = new DrugShelf();
        shelf2.setId(UUID.randomUUID());
        shelf2.setShelfCode("B-01");
        shelf2.setWard(otherWard);

        when(wardRepository.findById(otherWard.getId())).thenReturn(Optional.of(otherWard));
        when(drugSkuRepository.findById(drugSku.getId())).thenReturn(Optional.of(drugSku));
        when(drugShelfRepository.findByWardId(otherWard.getId())).thenReturn(List.of(shelf2));

        var batch = createBatch("B020", LocalDate.now().plusDays(90), 50);
        when(drugBatchRepository.findFefoCandidates(drugSku.getId(), shelf2.getId(), LocalDate.now()))
            .thenReturn(List.of(batch));
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new DispenseRequest(otherWard.getId(), drugSku.getId(), 10, "Test");
        assertDoesNotThrow(() -> dispenseService.dispense(request, managerPrincipal));
    }

    private DrugBatch createBatch(String batchNumber, LocalDate expiry, int qty) {
        var batch = new DrugBatch();
        batch.setId(UUID.randomUUID());
        batch.setBatchNumber(batchNumber);
        batch.setDrugSku(drugSku);
        batch.setShelf(shelf);
        batch.setExpiryDate(expiry);
        batch.setQuantityOnHand(qty);
        return batch;
    }
}
