package com.example.demo.event;

import com.example.demo.model.Member;
import com.example.demo.repository.MemberRepository;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TargetedNotificationEventListener {

    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTargetedNotification(TargetedNotificationEvent event) {
        if (event.getMemberIds().isEmpty()) return;
        // Church-scoped lookup — a stale or cross-church id silently resolves to nothing
        List<Member> members = memberRepository.findByChurchIdAndIdIn(event.getChurchId(), event.getMemberIds());
        int sent = notificationService.notifyMembers(members, event.getTitle(), event.getBody());
        log.debug("Targeted notification '{}' dispatched to {}/{} member(s)", event.getTitle(), sent, members.size());
    }
}
