package com.example.demo.scheduler;

import com.example.demo.model.Church;
import com.example.demo.model.GivingSchedule;
import com.example.demo.model.OnlinePaymentType;
import com.example.demo.repository.GivingScheduleRepository;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Each day, reminds members whose giving schedule falls due today. Mobile Money
 * cannot be silently auto-debited, so a schedule is a one-tap reminder to give,
 * not a background charge. `lastRunMonth` stops a member being reminded twice.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GivingReminderScheduler {

    private static final int PAGE_SIZE = 500;

    private final GivingScheduleRepository scheduleRepository;
    private final NotificationService notificationService;

    // Daily at 9:00am
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendDueGivingReminders() {
        int today = LocalDate.now().getDayOfMonth();
        if (today > 28) return; // schedules only use days 1–28
        String currentMonth = YearMonth.now().toString();

        int page = 0;
        Page<GivingSchedule> chunk;
        do {
            chunk = scheduleRepository.findByActiveTrueAndDayOfMonth(today, PageRequest.of(page++, PAGE_SIZE, Sort.by("id")));
            for (GivingSchedule s : chunk.getContent()) {
                if (currentMonth.equals(s.getLastRunMonth())) continue;   // already reminded this month
                Church church = s.getChurch();
                if (church != null && church.getDeletedAt() != null) continue; // skip churches in grace period

                String body = String.format(
                        "Your monthly %s of GHS %.2f is ready. Open Klink to give with one tap.",
                        label(s.getPaymentType()), s.getAmount());
                notificationService.notifyMember(s.getMember(), "Giving reminder", body);

                s.setLastRunMonth(currentMonth);
                scheduleRepository.save(s);
            }
        } while (chunk.hasNext());
    }

    private String label(OnlinePaymentType type) {
        return switch (type) {
            case TITHE -> "tithe";
            case OFFERING -> "offering";
            case WELFARE -> "welfare";
            case BUILDING_FUND -> "building fund gift";
            case MISSIONS -> "missions gift";
            default -> "giving";
        };
    }
}
