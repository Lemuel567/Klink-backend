package com.example.demo.service;

import com.example.demo.dto.request.CreatePollRequest;
import com.example.demo.dto.request.VoteRequest;
import com.example.demo.dto.response.PollResponse;
import com.example.demo.dto.response.PollResultsResponse;
import com.example.demo.model.*;
import com.example.demo.repository.PollRepository;
import com.example.demo.repository.PollVoteRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PollService {

    private final PollRepository pollRepository;
    private final PollVoteRepository pollVoteRepository;

    public PollResponse createPoll(CreatePollRequest request, MemberPrincipal principal) {
        // Poll creation is deliberately narrower than other content: Pastor or
        // Manager only. Polls are immutable once created (no edit endpoint
        // exists by design) — deletion remains the only way to retract one.
        RoleChecker.requirePastorOrManager(principal);

        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required");
        }
        if (request.getOptions() == null || request.getOptions().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A poll must have at least 2 options");
        }

        Poll poll = Poll.builder()
                .church(principal.getMember().getChurch())
                .question(request.getQuestion())
                .options(request.getOptions())
                .closesAt(request.getClosesAt())
                .createdBy(principal.getMemberId())
                .build();

        return PollResponse.from(pollRepository.save(poll), false);
    }

    @Transactional(readOnly = true)
    public Page<PollResponse> getAllPolls(MemberPrincipal principal, Pageable pageable) {
        return pollRepository.findByChurchIdOrderByCreatedAtDesc(principal.getChurchId(), pageable)
                .map(poll -> {
                    boolean voted = pollVoteRepository.existsByPollIdAndMemberId(
                            poll.getId(), principal.getMemberId());
                    return PollResponse.from(poll, voted);
                });
    }

    public PollResponse vote(UUID pollId, VoteRequest request, MemberPrincipal principal) {
        Poll poll = pollRepository.findByChurchIdAndId(principal.getChurchId(), pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        if (poll.getClosesAt() != null && poll.getClosesAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This poll has already closed");
        }

        if (pollVoteRepository.existsByPollIdAndMemberId(pollId, principal.getMemberId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already voted on this poll");
        }

        if (request.getSelectedOption() == null || request.getSelectedOption().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected option is required");
        }

        if (!poll.getOptions().contains(request.getSelectedOption())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Selected option is not valid for this poll");
        }

        PollVote vote = PollVote.builder()
                .church(principal.getMember().getChurch())
                .poll(poll)
                .member(principal.getMember())
                .selectedOption(request.getSelectedOption())
                .votedAt(LocalDateTime.now())
                .build();

        pollVoteRepository.save(vote);

        return PollResponse.from(poll, true);
    }

    public void deletePoll(UUID pollId, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        Poll poll = pollRepository.findByChurchIdAndId(principal.getChurchId(), pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        pollVoteRepository.deleteByPollId(pollId);
        pollRepository.delete(poll);
    }

    @Transactional(readOnly = true)
    public PollResultsResponse getResults(UUID pollId, MemberPrincipal principal) {
        // Results are visible to EVERY church member (votes stay anonymous —
        // only aggregate counts and percentages leave this method).
        Poll poll = pollRepository.findByChurchIdAndId(principal.getChurchId(), pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        List<PollVote> votes = pollVoteRepository.findByChurchIdAndPollId(
                principal.getChurchId(), pollId);

        int totalVotes = votes.size();

        Map<String, Long> voteCounts = votes.stream()
                .collect(Collectors.groupingBy(PollVote::getSelectedOption, Collectors.counting()));

        List<PollResultsResponse.OptionResult> results = poll.getOptions().stream()
                .map(option -> {
                    long count = voteCounts.getOrDefault(option, 0L);
                    double percentage = totalVotes > 0
                            ? Math.round((count * 100.0 / totalVotes) * 10.0) / 10.0
                            : 0.0;
                    return PollResultsResponse.OptionResult.builder()
                            .option(option)
                            .votes((int) count)
                            .percentage(percentage)
                            .build();
                })
                .toList();

        return PollResultsResponse.builder()
                .pollId(poll.getId())
                .question(poll.getQuestion())
                .totalVotes(totalVotes)
                .results(results)
                .build();
    }
}
