package com.example.demo.repository;

import com.example.demo.model.GroupMember;
import com.example.demo.model.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    @EntityGraph(attributePaths = "member")
    List<GroupMember> findByGroupId(UUID groupId);

    List<GroupMember> findByChurchId(UUID churchId);

    List<GroupMember> findByChurchIdAndMemberId(UUID churchId, UUID memberId);

    boolean existsByGroupIdAndMemberId(UUID groupId, UUID memberId);

    long countByGroupId(UUID groupId);

    void deleteByGroupIdAndMemberId(UUID groupId, UUID memberId);

    // Members of a group who have NOT paid dues for the given month.
    // Excludes only CONFIRMED payments — PENDING (auto-generated) records still count as unpaid.
    // NOT EXISTS instead of NOT IN: a single NULL member_id in the subquery would make
    // NOT IN return no rows at all, silently emptying the defaulters list.
    @Query("""
        SELECT gm.member FROM GroupMember gm
        WHERE gm.group.id = :groupId
          AND NOT EXISTS (
              SELECT 1 FROM Payment p
              WHERE p.group.id = :groupId
                AND p.member.id = gm.member.id
                AND p.paymentType = com.example.demo.model.PaymentType.DUES
                AND p.paymentMonth = :paymentMonth
                AND p.status = com.example.demo.model.PaymentStatus.CONFIRMED
          )
        """)
    List<Member> findGroupDuesDefaulters(
            @Param("groupId") UUID groupId,
            @Param("paymentMonth") String paymentMonth
    );

    // Distinct member IDs that belong to any of the given groups in the church
    @Query("""
        SELECT DISTINCT gm.member FROM GroupMember gm
        WHERE gm.church.id = :churchId
          AND gm.group.id IN :groupIds
        """)
    List<Member> findMembersByGroupIdsInChurch(
            @Param("churchId") UUID churchId,
            @Param("groupIds") List<UUID> groupIds
    );

    // Group IDs that a specific member belongs to
    @Query("SELECT gm.group.id FROM GroupMember gm WHERE gm.church.id = :churchId AND gm.member.id = :memberId")
    List<UUID> findGroupIdsByChurchIdAndMemberId(
            @Param("churchId") UUID churchId,
            @Param("memberId") UUID memberId
    );

    // Group member count per group — used for GroupSummaryResponse
    @Query("SELECT gm.group.id, COUNT(gm) FROM GroupMember gm WHERE gm.group.id IN :groupIds GROUP BY gm.group.id")
    List<Object[]> countMembersByGroupIds(@Param("groupIds") List<UUID> groupIds);

    @Modifying
    @Query("DELETE FROM GroupMember gm WHERE gm.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
