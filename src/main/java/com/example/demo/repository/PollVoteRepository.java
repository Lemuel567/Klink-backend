package com.example.demo.repository;

import com.example.demo.model.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {

    List<PollVote> findByChurchIdAndPollId(UUID churchId, UUID pollId);

    // Aggregate in SQL — loading every vote row into memory to count it made
    // results cost O(voters) heap and transfer on a table that only grows.
    @Query("""
        SELECT pv.selectedOption AS option, COUNT(pv) AS votes
        FROM PollVote pv
        WHERE pv.church.id = :churchId AND pv.poll.id = :pollId
        GROUP BY pv.selectedOption
        """)
    List<OptionCount> countVotesByOption(@Param("churchId") UUID churchId, @Param("pollId") UUID pollId);

    interface OptionCount {
        String getOption();
        long getVotes();
    }

    boolean existsByPollIdAndMemberId(UUID pollId, UUID memberId);

    // The caller's own vote — used to change a vote (upsert) and to tell the
    // client which option they currently hold so the UI can highlight it.
    Optional<PollVote> findByPollIdAndMemberId(UUID pollId, UUID memberId);

    void deleteByPollId(UUID pollId);

    @Modifying
    @Query("DELETE FROM PollVote pv WHERE pv.church.id = :churchId")
    void deleteAllByChurchId(@Param("churchId") UUID churchId);
}
