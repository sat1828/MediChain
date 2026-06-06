package com.medichain.domain.repository;

import com.medichain.domain.entity.DrugShelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DrugShelfRepository extends JpaRepository<DrugShelf, UUID> {

    List<DrugShelf> findByWardId(UUID wardId);
}
