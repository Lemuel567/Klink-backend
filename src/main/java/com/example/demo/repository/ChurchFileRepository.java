package com.example.demo.repository;

import com.example.demo.model.ChurchFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChurchFileRepository extends JpaRepository<ChurchFile, UUID> {

    List<ChurchFile> findByChurchId(UUID churchId);

    Page<ChurchFile> findByChurchId(UUID churchId, Pageable pageable);

    List<ChurchFile> findByChurchIdAndCategory(UUID churchId, String category);

    Page<ChurchFile> findByChurchIdAndCategory(UUID churchId, String category, Pageable pageable);

    List<ChurchFile> findByChurchIdAndLanguage(UUID churchId, String language);

    Page<ChurchFile> findByChurchIdAndLanguage(UUID churchId, String language, Pageable pageable);

    List<ChurchFile> findByChurchIdAndCategoryAndLanguage(UUID churchId, String category, String language);

    Page<ChurchFile> findByChurchIdAndCategoryAndLanguage(UUID churchId, String category, String language, Pageable pageable);

    Optional<ChurchFile> findByChurchIdAndId(UUID churchId, UUID id);

    long countByChurchId(UUID churchId);

    @Modifying
    @Query("DELETE FROM ChurchFile cf WHERE cf.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
