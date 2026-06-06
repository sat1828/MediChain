package com.medichain.domain.repository;

import com.medichain.domain.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findByPreferredTrue();

    @Query("SELECT v FROM Vendor v WHERE v.isActive = true ORDER BY v.name ASC")
    List<Vendor> findAllActive();
}
