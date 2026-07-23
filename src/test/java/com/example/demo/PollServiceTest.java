package com.example.demo;

import com.example.demo.dto.request.VoteRequest;
import com.example.demo.dto.response.PollResponse;
import com.example.demo.dto.response.PollResultsResponse;
import com.example.demo.model.*;
import com.example.demo.repository.PollRepository;
import com.example.demo.repository.PollVoteRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.PollService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock PollRepository pollRepository;
    @Mock PollVoteRepository pollVoteRepository;

    @InjectMocks PollService pollService;

    private MemberPrincipal principal(Role role) {
        UUID churchId = UUID.randomUUID();
        Church church = Church.builder().id(churchId).churchName("Test Church").build();
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .church(church)
                .role(role)
                .status(MemberStatus.ACTIVE)
                .fullName("Test " + role)
                .build();
        return new MemberPrincipal(member, churchId);
    }

    private Poll poll(UUID churchId, LocalDateTime closesAt) {
        return Poll.builder()
                .id(UUID.randomUUID())
                .church(Church.builder().id(churchId).build())
                .question("Service time?")
                .options(List.of("Morning", "Evening"))
                .closesAt(closesAt)
                .build();
    }

    private VoteRequest voteFor(String option) {
        VoteRequest request = new VoteRequest();
        ReflectionTestUtils.setField(request, "selectedOption", option);
        return request;
    }

    // ── vote (upsert) ─────────────────────────────────────────────────────────

    @Test
    void vote_firstTime_createsVoteWithSelectedOption() {
        MemberPrincipal p = principal(Role.MEMBER);
        Poll poll = poll(p.getChurchId(), null);
        when(pollRepository.findByChurchIdAndId(p.getChurchId(), poll.getId())).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPollIdAndMemberId(poll.getId(), p.getMemberId())).thenReturn(Optional.empty());

        PollResponse response = pollService.vote(poll.getId(), voteFor("Morning"), p);

        ArgumentCaptor<PollVote> saved = ArgumentCaptor.forClass(PollVote.class);
        verify(pollVoteRepository).save(saved.capture());
        assertThat(saved.getValue().getSelectedOption()).isEqualTo("Morning");
        assertThat(response.isVoted()).isTrue();
        assertThat(response.getVotedOption()).isEqualTo("Morning");
    }

    @Test
    void vote_again_updatesExistingRowInsteadOfInsertingOrRejecting() {
        MemberPrincipal p = principal(Role.MEMBER);
        Poll poll = poll(p.getChurchId(), null);
        PollVote existing = PollVote.builder()
                .id(UUID.randomUUID())
                .poll(poll)
                .member(p.getMember())
                .selectedOption("Morning")
                .build();
        when(pollRepository.findByChurchIdAndId(p.getChurchId(), poll.getId())).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByPollIdAndMemberId(poll.getId(), p.getMemberId())).thenReturn(Optional.of(existing));

        PollResponse response = pollService.vote(poll.getId(), voteFor("Evening"), p);

        // The SAME row is reused — the unique (poll, member) constraint is never violated
        verify(pollVoteRepository).save(same(existing));
        assertThat(existing.getSelectedOption()).isEqualTo("Evening");
        assertThat(response.getVotedOption()).isEqualTo("Evening");
    }

    @Test
    void vote_closedPoll_returnsConflict() {
        MemberPrincipal p = principal(Role.MEMBER);
        Poll poll = poll(p.getChurchId(), LocalDateTime.now().minusDays(1));
        when(pollRepository.findByChurchIdAndId(p.getChurchId(), poll.getId())).thenReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.vote(poll.getId(), voteFor("Morning"), p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(pollVoteRepository, never()).save(any());
    }

    @Test
    void vote_optionNotInPoll_returnsBadRequest() {
        MemberPrincipal p = principal(Role.MEMBER);
        Poll poll = poll(p.getChurchId(), null);
        when(pollRepository.findByChurchIdAndId(p.getChurchId(), poll.getId())).thenReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.vote(poll.getId(), voteFor("Afternoon"), p))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(pollVoteRepository, never()).save(any());
    }

    // ── results (in-memory count from the church-scoped finder) ───────────────

    private PollVote voteRow(String option) {
        return PollVote.builder().id(UUID.randomUUID()).selectedOption(option).build();
    }

    @Test
    void getResults_aggregatesCountsAndPercentages_includingZeroVoteOptions() {
        MemberPrincipal p = principal(Role.MEMBER);
        Poll poll = Poll.builder()
                .id(UUID.randomUUID())
                .church(Church.builder().id(p.getChurchId()).build())
                .question("Service time?")
                .options(List.of("Morning", "Evening", "Online"))
                .build();
        when(pollRepository.findByChurchIdAndId(p.getChurchId(), poll.getId())).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByChurchIdAndPollId(p.getChurchId(), poll.getId()))
                .thenReturn(List.of(voteRow("Morning"), voteRow("Morning"), voteRow("Morning"), voteRow("Evening")));

        PollResultsResponse results = pollService.getResults(poll.getId(), p);

        assertThat(results.getTotalVotes()).isEqualTo(4);
        assertThat(results.getResults()).hasSize(3);
        assertThat(results.getResults().get(0).getVotes()).isEqualTo(3);
        assertThat(results.getResults().get(0).getPercentage()).isEqualTo(75.0);
        assertThat(results.getResults().get(1).getPercentage()).isEqualTo(25.0);
        assertThat(results.getResults().get(2).getVotes()).isZero();
        assertThat(results.getResults().get(2).getPercentage()).isZero();
    }

    @Test
    void getResults_noVotes_allZeroWithoutDivisionByZero() {
        MemberPrincipal p = principal(Role.MEMBER);
        Poll poll = poll(p.getChurchId(), null);
        when(pollRepository.findByChurchIdAndId(p.getChurchId(), poll.getId())).thenReturn(Optional.of(poll));
        when(pollVoteRepository.findByChurchIdAndPollId(p.getChurchId(), poll.getId())).thenReturn(List.of());

        PollResultsResponse results = pollService.getResults(poll.getId(), p);

        assertThat(results.getTotalVotes()).isZero();
        assertThat(results.getResults()).allSatisfy(r -> {
            assertThat(r.getVotes()).isZero();
            assertThat(r.getPercentage()).isZero();
        });
    }
}
