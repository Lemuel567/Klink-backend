package com.example.demo.service;

import com.example.demo.dto.response.AnalyticsDashboardResponse;
import com.example.demo.model.AttendanceStatus;
import com.example.demo.model.MemberStatus;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.CollectionTotal;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AttendanceRepository attendanceRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;

    public AnalyticsDashboardResponse getDashboard(int months, MemberPrincipal principal) {
        // Full oversight incl. finances → Pastor or Elder (Manager can't see money).
        RoleChecker.requirePastorOrElder(principal);

        int n = Math.min(Math.max(months, 3), 12);
        UUID church = principal.getChurchId();
        YearMonth current = YearMonth.now();

        List<String> labels = new ArrayList<>();
        List<Long> attendance = new ArrayList<>();
        List<BigDecimal> giving = new ArrayList<>();
        List<Long> newMembers = new ArrayList<>();

        for (int i = n - 1; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            LocalDate from = ym.atDay(1);
            LocalDate to = ym.atEndOfMonth();

            labels.add(ym.toString());
            attendance.add(attendanceRepository.countByChurchIdAndStatusAndServiceDateBetween(
                    church, AttendanceStatus.PRESENT, from, to));

            BigDecimal monthGiving = BigDecimal.ZERO;
            for (CollectionTotal ct : paymentRepository.summariseCollections(church, from, to)) {
                if (ct.getManualTotal() != null) monthGiving = monthGiving.add(ct.getManualTotal());
                if (ct.getOnlineTotal() != null) monthGiving = monthGiving.add(ct.getOnlineTotal());
            }
            giving.add(monthGiving);

            // Exclusive upper bound: [1st 00:00, next-month-1st 00:00) — the old
            // inclusive 23:59:59 cap dropped members created in the final second
            // of the month from BOTH months' counts.
            newMembers.add(memberRepository.countByChurchIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    church, from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
        }

        int last = labels.size() - 1;
        long thisMonthAttendance = attendance.get(last);
        BigDecimal thisMonthGiving = giving.get(last);
        long prevAttendance = last > 0 ? attendance.get(last - 1) : 0;
        BigDecimal prevGiving = last > 0 ? giving.get(last - 1) : BigDecimal.ZERO;

        return AnalyticsDashboardResponse.builder()
                .months(labels)
                .attendance(attendance)
                .giving(giving)
                .newMembers(newMembers)
                .totalMembers(memberRepository.countByChurchId(church))
                .activeMembers(memberRepository.countByChurchIdAndStatus(church, MemberStatus.ACTIVE))
                .thisMonthAttendance(thisMonthAttendance)
                .thisMonthGiving(thisMonthGiving)
                .attendanceGrowthPct(pctChange(prevAttendance, thisMonthAttendance))
                .givingGrowthPct(pctChange(prevGiving, thisMonthGiving))
                .newMembersThisMonth(newMembers.get(last))
                .build();
    }

    private double pctChange(long prev, long now) {
        return pctChange(BigDecimal.valueOf(prev), BigDecimal.valueOf(now));
    }

    private double pctChange(BigDecimal prev, BigDecimal now) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) {
            return now != null && now.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return now.subtract(prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(prev, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
