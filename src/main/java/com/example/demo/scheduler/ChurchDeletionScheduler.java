package com.example.demo.scheduler;

import com.example.demo.model.Church;
import com.example.demo.repository.ChurchRepository;
import com.example.demo.service.ChurchDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChurchDeletionScheduler {

    private final ChurchRepository churchRepository;
    private final ChurchDeletionService churchDeletionService;

    // Runs daily at 2am — finds churches whose 30-day grace period has expired and permanently deletes them
    @Scheduled(cron = "0 0 2 * * *")
    public void purgeExpiredChurches() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<Church> expired = churchRepository.findChurchesPastGracePeriod(cutoff);

        if (expired.isEmpty()) {
            return;
        }

        log.info("Purging {} church(es) past 30-day grace period", expired.size());

        for (Church church : expired) {
            try {
                churchDeletionService.permanentlyDelete(church);
            } catch (Exception e) {
                log.error("Failed to permanently delete church {} ({}): {}",
                        church.getChurchName(), church.getId(), e.getMessage(), e);
            }
        }
    }
}
