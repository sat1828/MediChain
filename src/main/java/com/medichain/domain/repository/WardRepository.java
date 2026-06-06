package com.medichain.domain.repository;

import com.medichain.domain.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WardRepository extends JpaRepository<Ward, UUID> {

    List<Ward> findByHospitalId(UUID hospitalId);

    @Query("SELECT w FROM Ward w JOIN FETCH w.hospital WHERE w.id = :wardId")
    java.util.Optional<Ward> findByIdWithHospital(@Param("wardId") UUID wardId);
}
