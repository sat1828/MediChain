package com.medichain.domain.entity;

public final class Enums {

    private Enums() {}

    public enum TransactionType {
        RECEIPT, DISPENSE, TRANSFER, ADJUSTMENT, WASTE, NGO_TRANSFER
    }

    public enum StorageCondition {
        AMBIENT, REFRIGERATED, CONTROLLED
    }

    public enum ProcurementStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, DISPATCHED, RECEIVED, CANCELLED
    }

    public enum AlertSeverity {
        CRITICAL, WARNING, WATCH, INFO
    }

    public enum UserRole {
        PHARMACY_MANAGER, WARD_PHARMACIST, PROCUREMENT_OFFICER, NGO_PARTNER, ADMIN
    }

    public enum VEDClassification {
        VITAL, ESSENTIAL, DESIRABLE
    }

    public enum ABCClassification {
        A, B, C
    }

    public enum AlertType {
        STOCKOUT, EXPIRY_WARNING, EXPIRY_CRITICAL
    }

    public enum NGOTransferStatus {
        DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, COMPLETED
    }
}
