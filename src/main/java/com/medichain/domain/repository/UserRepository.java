package com.medichain.domain.repository;

import com.medichain.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u JOIN FETCH u.hospital LEFT JOIN FETCH u.ward WHERE u.username = :username")
    Optional<User> findByUsernameWithAssociations(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.hospital.id = :hospitalId AND u.isActive = true")
    java.util.List<User> findByHospitalId(@Param("hospitalId") UUID hospitalId);
}
