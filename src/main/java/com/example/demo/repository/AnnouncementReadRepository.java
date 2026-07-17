package com.example.demo.repository;

import com.example.demo.model.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, UUID> {

    boolean existsByAnnouncementIdAndMemberId(UUID announcementId, UUID memberId);

    // Which of the given announcements this member has already read (for the read/unread flag).
    @Query("""
        SELECT ar.announcement.id FROM AnnouncementRead ar
        WHERE ar.church.id = :churchId
          AND ar.memberId = :memberId
          AND ar.announcement.id IN :announcementIds
        """)
    List<UUID> findReadAnnouncementIds(@Param("churchId") UUID churchId,
                                       @Param("memberId") UUID memberId,
                                       @Param("announcementIds") List<UUID> announcementIds);

    @Modifying
    @Query("DELETE FROM AnnouncementRead ar WHERE ar.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
