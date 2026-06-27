package com.example.demo.scheduler;

import com.example.demo.model.Pledge;
import com.example.demo.model.PledgeStatus;
import java.math.BigDecimal;
import com.example.demo.repository.PledgeRepository;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PledgeReminderScheduler {

    private static final int PAGE_SIZE = 500;

    private final PledgeRepository pledgeRepository;
    private final NotificationService notificationService;

    // Runs on the 1st of every month at 8am
    @Scheduled(cron = "0 0 8 1 * *")
    @Transactional(readOnly = true)
    public void sendMonthlyPledgeReminders() {
        List<PledgeStatus> unpaidStatuses = List.of(PledgeStatus.UNPAID, PledgeStatus.PARTIALLY_PAID);
        int page = 0;
        Page<Pledge> chunk;

        do {
            chunk = pledgeRepository.findByStatusInPaged(unpaidStatuses, PageRequest.of(page++, PAGE_SIZE));
            for (Pledge pledge : chunk.getContent()) {
                String label = pledge.getDescription() != null ? pledge.getDescription() : "General Pledge";
                BigDecimal paid = pledge.getAmountPaid() != null ? pledge.getAmountPaid() : BigDecimal.ZERO;
                String body = String.format(
                        "You have an outstanding pledge: %s. Paid: %.2f / %.2f. Please pay when you can.",
                        label, paid, pledge.getAmount());
                notificationService.notifyMember(pledge.getMember(), "Pledge Reminder", body);
            }
        } while (chunk.hasNext());
    }
}
