package com.example.demo.scheduler;

import com.example.demo.model.Group;
import com.example.demo.model.GroupMember;
import com.example.demo.model.GroupStatus;
import com.example.demo.model.Payment;
import com.example.demo.model.PaymentStatus;
import com.example.demo.model.PaymentType;
import com.example.demo.repository.GroupMemberRepository;
import com.example.demo.repository.GroupRepository;
import com.example.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creates the month's PENDING dues records for every active group member.
 * This job was specified (cron {@code 0 0 0 1 * *}) but never
 * implemented — only the manual {@code POST /groups/{id}/dues/generate}
 * endpoint existed, so no PENDING records were ever auto-created.
 * Idempotent: members who already have any record for the month are skipped,
 * so re-runs and the manual endpoint can coexist safely.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupDuesGenerationScheduler {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final PaymentRepository paymentRepository;

    // 1st of every month at midnight
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void generateMonthlyDues() {
        String month = YearMonth.now().toString();
        int created = 0;

        // Process active groups in pages of 50 to avoid loading all groups globally
        Pageable pageable = PageRequest.of(0, 50);
        Page<Group> page;
        do {
            page = groupRepository.findByStatus(GroupStatus.ACTIVE, pageable);

            for (Group group : page.getContent()) {
                // Churches in their 30-day deletion grace period get no new records
                if (group.getChurch().getDeletedAt() != null) continue;
                if (group.getDuesAmount() == null
                        || group.getDuesAmount().compareTo(BigDecimal.ZERO) <= 0) continue;

                Set<UUID> alreadyCovered = paymentRepository
                        .findByGroupIdAndPaymentMonth(group.getId(), month).stream()
                        .filter(p -> p.getMember() != null)
                        .map(p -> p.getMember().getId())
                        .collect(Collectors.toSet());

                for (GroupMember gm : groupMemberRepository.findByGroupId(group.getId())) {
                    if (alreadyCovered.contains(gm.getMember().getId())) continue;

                    paymentRepository.save(Payment.builder()
                            .church(group.getChurch())
                            .member(gm.getMember())
                            .group(group)
                            .paymentType(PaymentType.DUES)
                            .amount(group.getDuesAmount())
                            .paymentMonth(month)
                            .status(PaymentStatus.PENDING)
                            .build());
                    created++;
                }
            }

            pageable = pageable.next();
        } while (page.hasNext());

        log.info("[DUES] Generated {} pending group dues record(s) for {}", created, month);
    }
}
