package com.example.demo.service;

import com.example.demo.dto.request.CreateEventRequest;
import com.example.demo.dto.response.EventResponse;
import com.example.demo.model.Event;
import com.example.demo.repository.EventRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final EventRepository eventRepository;

    public EventResponse createEvent(CreateEventRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        if (request.getEventDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event date is required");
        }

        Event event = Event.builder()
                .church(principal.getMember().getChurch())
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .category(request.getCategory())
                .eventDate(request.getEventDate())
                .reminderSent(false)
                .createdBy(principal.getMemberId())
                .build();

        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getAllEvents(MemberPrincipal principal, Pageable pageable) {
        return eventRepository
                .findByChurchIdOrderByEventDateAsc(principal.getChurchId(), pageable)
                .map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(UUID eventId, MemberPrincipal principal) {
        Event event = eventRepository.findByChurchIdAndId(principal.getChurchId(), eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return EventResponse.from(event);
    }

    public void deleteEvent(UUID eventId, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);
        Event event = eventRepository.findByChurchIdAndId(principal.getChurchId(), eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        eventRepository.delete(event);
    }

}
