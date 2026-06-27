package com.example.demo.repository;

import com.example.demo.model.Facility;
import com.example.demo.model.FacilityCondition;
import com.example.demo.model.FacilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, UUID> {

    Page<Facility> findByChurchIdAndDeletedAtIsNull(UUID churchId, Pageable pageable);

    Page<Facility> findByChurchIdAndFacilityTypeAndDeletedAtIsNull(UUID churchId, FacilityType facilityType, Pageable pageable);

    Page<Facility> findByChurchIdAndConditionAndDeletedAtIsNull(UUID churchId, FacilityCondition condition, Pageable pageable);

    Page<Facility> findByChurchIdAndIsActiveAndDeletedAtIsNull(UUID churchId, boolean isActive, Pageable pageable);

    Page<Facility> findByChurchIdAndFacilityTypeAndIsActiveAndDeletedAtIsNull(UUID churchId, FacilityType facilityType, boolean isActive, Pageable pageable);

    Optional<Facility> findByChurchIdAndIdAndDeletedAtIsNull(UUID churchId, UUID id);

    @Modifying
    @Query("DELETE FROM Facility f WHERE f.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}