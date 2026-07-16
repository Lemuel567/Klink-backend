package com.example.demo.scheduler;

import com.example.demo.model.Member;
import com.example.demo.model.MemberStatus;
import com.example.demo.model.PaymentType;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.ChurchRepository;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WelfareReminderScheduler {

    private static final int CHURCH_PAGE_SIZE = 100;

    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    private final NotificationService notificationService;

    // Runs on the last day of every month at 9am
    @Scheduled(cron = "0 0 9 L * *")
    @Transactional(readOnly = true)
    public void sendWelfareReminders() {
        String currentMonth = YearMonth.now().toString();
        int page = 0;
        Page<com.example.demo.model.Church> chunk;

        do {
            chunk = churchRepository.findAll(PageRequest.of(page++, CHURCH_PAGE_SIZE));
            chunk.forEach(church -> {
                // Churches in their 30-day deletion grace period must not keep nagging members
                if (church.getDeletedAt() != null) return;
                List<Member> defaulters = memberRepository.findWelfareDefaulters(
                        church.getId(), currentMonth, MemberStatus.ACTIVE, PaymentType.WELFARE);
                for (Member member : defaulters) {
                    notificationService.notifyMember(
                            member,
                            "Welfare Reminder",
                            "You have not paid your welfare for " + currentMonth + ". Please pay before the end of the month."
                    );
                }
            });
        } while (chunk.hasNext());
    }
}
