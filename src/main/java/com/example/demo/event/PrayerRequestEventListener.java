package com.example.demo.event;

import com.example.demo.model.Member;
import com.example.demo.model.Role;
import com.example.demo.repository.MemberRepository;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PrayerRequestEventListener {

    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePrayerRequestCreated(PrayerRequestCreatedEvent event) {
        List<Member> leaders = memberRepository.findByChurchIdAndRoleIn(
                event.getChurchId(), List.of(Role.PASTOR, Role.ELDER));
        if (leaders.isEmpty()) return;

        String title = "New Prayer Request";
        String body = event.getRequesterName() + " shared: " + event.getTitle();
        notificationService.notifyMembers(leaders, title, body);
    }
}
