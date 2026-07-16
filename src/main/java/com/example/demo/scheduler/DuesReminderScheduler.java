package com.example.demo.scheduler;

import com.example.demo.model.Group;
import com.example.demo.model.GroupStatus;
import com.example.demo.model.Member;
import com.example.demo.repository.GroupMemberRepository;
import com.example.demo.repository.GroupRepository;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DuesReminderScheduler {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationService notificationService;

    // Runs on the last day of every month at 9:30am (offset from WelfareReminderScheduler at 9:00am)
    @Scheduled(cron = "0 30 9 L * *")
    @Transactional(readOnly = true)
    public void sendMonthlyDuesReminders() {
        String currentMonth = YearMonth.now().toString();

        // Process active groups in pages of 50 to avoid loading all groups globally into memory.
        Pageable pageable = PageRequest.of(0, 50);
        Page<Group> page;
        do {
            page = groupRepository.findByStatus(GroupStatus.ACTIVE, pageable);

            for (Group group : page.getContent()) {
                // Skip groups of churches in their 30-day deletion grace period
                if (group.getChurch().getDeletedAt() != null) continue;

                List<Member> defaulters = groupMemberRepository
                        .findGroupDuesDefaulters(group.getId(), currentMonth);

                for (Member member : defaulters) {
                    notificationService.notifyMember(
                            member,
                            "Group Dues Reminder",
                            "You have not paid your dues for " + group.getGroupName()
                                    + " for " + currentMonth + ". Please pay before the end of the month."
                    );
                }
            }

            pageable = pageable.next();
        } while (page.hasNext());
    }
}