package com.medichain.domain.repository;

import com.medichain.domain.entity.NGO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NGORepository extends JpaRepository<NGO, UUID> {

    @Query("SELECT n FROM NGO n WHERE n.verified = true AND n.isActive = true ORDER BY n.name ASC")
    List<NGO> findVerifiedNgos();

    @Query("SELECT n FROM NGO n WHERE n.hasColdChain = true AND n.verified = true AND n.isActive = true")
    List<NGO> findVerifiedWithColdChain();
}
