package com.example.demo.repository;

import com.example.demo.model.StoreItem;
import com.example.demo.model.StoreItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreItemRepository extends JpaRepository<StoreItem, UUID> {

    List<StoreItem> findByChurchId(UUID churchId);

    Page<StoreItem> findByChurchId(UUID churchId, Pageable pageable);

    List<StoreItem> findByChurchIdAndStatus(UUID churchId, StoreItemStatus status);

    Optional<StoreItem> findByChurchIdAndId(UUID churchId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StoreItem s WHERE s.church.id = :churchId AND s.id = :id")
    Optional<StoreItem> findByChurchIdAndIdForUpdate(@Param("churchId") UUID churchId, @Param("id") UUID id);

    @Modifying
    @Query("DELETE FROM StoreItem si WHERE si.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
