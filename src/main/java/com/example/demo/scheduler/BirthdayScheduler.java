package com.example.demo.scheduler;

import com.example.demo.model.Member;
import com.example.demo.model.MemberStatus;
import com.example.demo.repository.MemberRepository;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BirthdayScheduler {

    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    // Runs every morning at 8am.
    // Single cross-church query — on any given day birthday members are ~0.3% of total,
    // so the result set is tiny regardless of church count.
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void sendBirthdayGreetings() {
        LocalDate today = LocalDate.now();
        List<Member> birthdayMembers = memberRepository.findAllBirthdayMembersGlobal(
                today.getMonthValue(), today.getDayOfMonth(), MemberStatus.ACTIVE);

        for (Member member : birthdayMembers) {
            // Skip members of churches in their 30-day deletion grace period
            if (member.getChurch().getDeletedAt() != null) continue;
            String firstName = member.getFullName().split(" ")[0];
            notificationService.notifyMember(
                    member,
                    "Happy Birthday, " + firstName + "!",
                    "Wishing you a blessed birthday from " + member.getChurch().getChurchName() +
                            ". May God bless you abundantly!");
        }

        log.info("[BIRTHDAY] Sent greetings to {} member(s) on {}", birthdayMembers.size(), today);
    }
}
