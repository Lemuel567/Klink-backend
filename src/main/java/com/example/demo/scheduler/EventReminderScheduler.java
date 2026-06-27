package com.example.demo.scheduler;

import com.example.demo.model.Event;
import com.example.demo.repository.EventRepository;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventReminderScheduler {

    private final EventRepository eventRepository;
    private final NotificationService notificationService;

    // Runs every day at 8am
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in48Hours = now.plusHours(48);

        List<Event> upcomingEvents = eventRepository
                .findByReminderSentFalseAndEventDateBetween(now, in48Hours);

        for (Event event : upcomingEvents) {
            String dateStr = event.getEventDate().format(DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a"));
            String title = "Upcoming Event: " + event.getTitle();
            String body = event.getDescription() != null
                    ? event.getDescription() + " — " + dateStr
                    : dateStr;

            int notified = notificationService.notifyAllMembers(event.getChurch().getId(), title, body);

            // Only mark as sent if at least one notification was dispatched.
            // If notified == 0 (e.g. FCM globally down), leave reminderSent=false so the next run retries.
            if (notified > 0) {
                event.setReminderSent(true);
                eventRepository.save(event);
            } else {
                log.warn("Event reminder for '{}' (id={}) not dispatched — will retry on next run",
                        event.getTitle(), event.getId());
            }
        }
    }
}