package com.example.demo.repository;

import com.example.demo.model.Group;
import com.example.demo.model.GroupStatus;
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
public interface GroupRepository extends JpaRepository<Group, UUID> {

    List<Group> findByChurchId(UUID churchId);

    Page<Group> findByChurchId(UUID churchId, Pageable pageable);

    Optional<Group> findByChurchIdAndId(UUID churchId, UUID id);

    List<Group> findByChurchIdAndStatus(UUID churchId, GroupStatus status);

    List<Group> findByStatus(GroupStatus status);

    Page<Group> findByStatus(GroupStatus status, Pageable pageable);

    // Groups the caller belongs to — as group admin, group fin sec, or regular member
    @Query("""
        SELECT g FROM ChurchGroup g
        WHERE g.church.id = :churchId
          AND (
              (g.groupAdmin IS NOT NULL AND g.groupAdmin.id = :memberId)
              OR (g.groupFinSec IS NOT NULL AND g.groupFinSec.id = :memberId)
              OR EXISTS (
                  SELECT 1 FROM GroupMember gm
                  WHERE gm.group.id = g.id AND gm.member.id = :memberId
              )
          )
        """)
    Page<Group> findGroupsByMembership(
            @Param("churchId") UUID churchId,
            @Param("memberId") UUID memberId,
            Pageable pageable);

    @Modifying
    @Query("DELETE FROM ChurchGroup g WHERE g.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
