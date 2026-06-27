package com.example.demo.repository;

import com.example.demo.model.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {

    List<PollVote> findByChurchIdAndPollId(UUID churchId, UUID pollId);

    boolean existsByPollIdAndMemberId(UUID pollId, UUID memberId);

    void deleteByPollId(UUID pollId);

    @Modifying
    @Query("DELETE FROM PollVote pv WHERE pv.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
