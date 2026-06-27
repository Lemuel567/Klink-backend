package com.example.demo.repository;

import com.example.demo.model.Church;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChurchRepository extends JpaRepository<Church, UUID> {

    Optional<Church> findByChurchCode(String churchCode);

    boolean existsByChurchCode(String churchCode);

    @Query("SELECT c FROM Church c WHERE c.deletedAt IS NOT NULL AND c.deletedAt < :cutoff")
    List<Church> findChurchesPastGracePeriod(@Param("cutoff") LocalDateTime cutoff);
}
