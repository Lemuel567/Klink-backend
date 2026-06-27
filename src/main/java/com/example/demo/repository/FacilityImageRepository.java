package com.example.demo.repository;

import com.example.demo.model.FacilityImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacilityImageRepository extends JpaRepository<FacilityImage, UUID> {

    List<FacilityImage> findByFacilityIdOrderBySortOrderAscUploadedAtAsc(UUID facilityId);

    Optional<FacilityImage> findByIdAndFacilityId(UUID id, UUID facilityId);

    @Modifying
    @Query("UPDATE FacilityImage fi SET fi.isPrimary = false WHERE fi.facility.id = :facilityId")
    void clearPrimaryForFacility(@Param("facilityId") UUID facilityId);

    List<FacilityImage> findByChurchId(UUID churchId);

    @Modifying
    @Query("DELETE FROM FacilityImage fi WHERE fi.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}