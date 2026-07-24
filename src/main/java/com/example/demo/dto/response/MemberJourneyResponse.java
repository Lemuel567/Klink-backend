package com.example.demo.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * "Your Journey" — a personal summary of ONE member's own activity at the
 * church. Every field is the requesting member's own data, assembled by
 * MemberJourneyService strictly from their id in the JWT. Nothing here ever
 * describes another member.
 */
@Getter
@Builder
public class MemberJourneyResponse {

    private String fullName;
    private String photoUrl;
    private LocalDate memberSince;

    private BigDecimal totalGiven;
    private BigDecimal givenThisYear;

    private long servicesAttended;

    private int pledgesTotal;
    private int pledgesKept;
    private BigDecimal pledgedAmount;
    private BigDecimal pledgePaidAmount;

    private long projectsSupported;
    private BigDecimal projectContributed;

    // welfareApplicable=false when the church doesn't use welfare — the client
    // then hides the welfare card instead of showing a misleading "Due".
    private boolean welfareApplicable;
    private boolean welfareUpToDate;

    private List<String> groups;

    // Consecutive months (ending this month, or last month as grace) the member
    // has given something. 0 = no current streak.
    private int givingStreakMonths;

    // Earned "faithfulness" badges — celebratory, never comparative.
    private List<Milestone> milestones;

    @Getter
    @Builder
    public static class Milestone {
        private String icon;
        private String label;
    }
}
