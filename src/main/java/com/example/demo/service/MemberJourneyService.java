package com.example.demo.service;

import com.example.demo.dto.response.MemberJourneyResponse;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Builds the "Your Journey" personal summary. SAFETY: the member id and church
 * id come ONLY from the authenticated principal (the JWT) — never from any
 * request parameter — so a member can only ever assemble their OWN summary.
 * There is no code path here that reads another member's data.
 */
@Service
@RequiredArgsConstructor
public class MemberJourneyService {

    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final AttendanceRepository attendanceRepository;
    private final PledgeRepository pledgeRepository;
    private final ProjectContributionRepository projectContributionRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional(readOnly = true)
    public MemberJourneyResponse getMyJourney(MemberPrincipal principal) {
        UUID churchId = principal.getChurchId();
        UUID memberId = principal.getMemberId();

        Member me = memberRepository.findByChurchIdAndId(churchId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        LocalDate yearStart = LocalDate.now().withDayOfYear(1);
        BigDecimal totalGiven = nz(paymentRepository.sumMemberGiving(churchId, memberId));
        BigDecimal givenThisYear = nz(paymentRepository.sumMemberGivingSince(churchId, memberId, yearStart));

        long attended = attendanceRepository.countByChurchIdAndMemberIdAndStatus(
                churchId, memberId, AttendanceStatus.PRESENT);

        List<Pledge> pledges = pledgeRepository.findByChurchIdAndMemberId(churchId, memberId);
        int pledgesTotal = pledges.size();
        int pledgesKept = (int) pledges.stream().filter(p -> p.getStatus() == PledgeStatus.PAID).count();
        BigDecimal pledgedAmount = pledges.stream()
                .map(p -> nz(p.getAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pledgePaidAmount = pledges.stream()
                .map(p -> nz(p.getAmountPaid())).reduce(BigDecimal.ZERO, BigDecimal::add);

        long projectsSupported = projectContributionRepository.countDistinctProjectsByMember(churchId, memberId);
        BigDecimal projectContributed = nz(projectContributionRepository.sumMemberContributions(churchId, memberId));

        // Welfare only makes sense if the church actually runs it.
        BigDecimal welfareAmount = me.getChurch().getWelfareAmount();
        boolean welfareApplicable = welfareAmount != null && welfareAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean welfareUpToDate = welfareApplicable && !paymentRepository
                .findByChurchIdAndMemberIdAndPaymentTypeAndPaymentMonth(
                        churchId, memberId, PaymentType.WELFARE, YearMonth.now().toString())
                .isEmpty();

        List<String> groups = groupMemberRepository.findByChurchIdAndMemberId(churchId, memberId).stream()
                .map(gm -> gm.getGroup().getGroupName())
                .toList();

        LocalDate memberSince = me.getMembershipDate() != null
                ? me.getMembershipDate()
                : (me.getCreatedAt() != null ? me.getCreatedAt().toLocalDate() : null);

        int givingStreak = givingStreakMonths(paymentRepository.findMemberGivingDates(churchId, memberId));
        List<MemberJourneyResponse.Milestone> milestones = milestones(
                memberSince, givingStreak, attended, pledgesKept, projectsSupported, groups.size());

        return MemberJourneyResponse.builder()
                .fullName(me.getFullName())
                .photoUrl(me.getPhotoUrl())
                .memberSince(memberSince)
                .totalGiven(totalGiven)
                .givenThisYear(givenThisYear)
                .servicesAttended(attended)
                .pledgesTotal(pledgesTotal)
                .pledgesKept(pledgesKept)
                .pledgedAmount(pledgedAmount)
                .pledgePaidAmount(pledgePaidAmount)
                .projectsSupported(projectsSupported)
                .projectContributed(projectContributed)
                .welfareApplicable(welfareApplicable)
                .welfareUpToDate(welfareUpToDate)
                .groups(groups)
                .givingStreakMonths(givingStreak)
                .milestones(milestones)
                .build();
    }

    /** Consecutive months up to now (with a one-month grace) the member gave. */
    private static int givingStreakMonths(List<LocalDate> givingDates) {
        java.util.Set<YearMonth> months = new java.util.HashSet<>();
        for (LocalDate d : givingDates) months.add(YearMonth.from(d));
        if (months.isEmpty()) return 0;

        YearMonth cursor = YearMonth.now();
        // Grace: not having given yet THIS month shouldn't break last month's streak.
        if (!months.contains(cursor)) cursor = cursor.minusMonths(1);

        int streak = 0;
        while (months.contains(cursor)) {
            streak++;
            cursor = cursor.minusMonths(1);
        }
        return streak;
    }

    /** Celebratory faithfulness badges from the member's own activity. */
    private static List<MemberJourneyResponse.Milestone> milestones(
            LocalDate memberSince, int givingStreak, long attended,
            int pledgesKept, long projectsSupported, int groupCount) {

        List<MemberJourneyResponse.Milestone> out = new java.util.ArrayList<>();

        if (memberSince != null) {
            long months = java.time.temporal.ChronoUnit.MONTHS.between(memberSince, LocalDate.now());
            long years = months / 12;
            if (years >= 1) {
                out.add(badge("🎖️", years == 1 ? "One year faithful" : years + " years faithful"));
            }
        }
        if (givingStreak >= 3) {
            out.add(badge("🔥", "Faithful giver · " + givingStreak + "-month streak"));
        }
        if (attended >= 50) out.add(badge("🏅", "Devoted worshipper"));
        else if (attended >= 20) out.add(badge("⛪", "Regular worshipper"));
        else if (attended >= 5) out.add(badge("🌱", "Growing in faith"));

        if (pledgesKept >= 1) out.add(badge("🤝", pledgesKept == 1 ? "Pledge keeper" : "Pledge keeper ×" + pledgesKept));
        if (projectsSupported >= 1) out.add(badge("🏗️", "Church builder"));
        if (groupCount >= 1) out.add(badge("👥", "Connected in fellowship"));

        return out;
    }

    private static MemberJourneyResponse.Milestone badge(String icon, String label) {
        return MemberJourneyResponse.Milestone.builder().icon(icon).label(label).build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
