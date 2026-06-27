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

    void deleteByGroupIdAndMemberId(UUID groupId, UUID memberId);

    // Members of a group who have NOT paid dues for the given month.
    // Excludes only CONFIRMED payments — PENDING (auto-generated) records still count as unpaid.
    @Query("""
        SELECT gm.member FROM GroupMember gm
        WHERE gm.group.id = :groupId
          AND gm.member.id NOT IN (
              SELECT p.member.id FROM Payment p
              WHERE p.group.id = :groupId
                AND p.paymentType = com.example.demo.model.PaymentType.DUES
                AND p.paymentMonth = :paymentMonth
                AND p.status = com.example.demo.model.PaymentStatus.CONFIRMED
          )
        """)
    List<Member> findGroupDuesDefaulters(
            @Param("groupId") UUID groupId,
            @Param("paymentMonth") String paymentMonth
    );

    @Modifying
    @Query("DELETE FROM GroupMember gm WHERE gm.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
