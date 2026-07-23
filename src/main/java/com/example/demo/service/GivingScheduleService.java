package com.example.demo.service;

import com.example.demo.dto.request.CreateGivingScheduleRequest;
import com.example.demo.dto.response.GivingScheduleResponse;
import com.example.demo.model.GivingSchedule;
import com.example.demo.model.OnlinePaymentType;
import com.example.demo.repository.GivingScheduleRepository;
import com.example.demo.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GivingScheduleService {

    private final GivingScheduleRepository repository;

    public GivingScheduleResponse create(CreateGivingScheduleRequest request, MemberPrincipal principal) {
        if (request.getPaymentType() == OnlinePaymentType.PROJECT_CONTRIBUTION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Project contributions can't be scheduled — give to the project directly.");
        }

        GivingSchedule schedule = GivingSchedule.builder()
                .church(principal.getMember().getChurch())
                .member(principal.getMember())
                .paymentType(request.getPaymentType())
                .amount(request.getAmount())
                .dayOfMonth(request.getDayOfMonth())
                .active(true)
                .build();

        return GivingScheduleResponse.from(repository.save(schedule));
    }

    @Transactional(readOnly = true)
    public List<GivingScheduleResponse> listMine(MemberPrincipal principal) {
        return repository.findByChurchIdAndMemberIdOrderByCreatedAtDesc(
                        principal.getChurchId(), principal.getMemberId())
                .stream()
                .map(GivingScheduleResponse::from)
                .toList();
    }

    public GivingScheduleResponse setActive(UUID id, boolean active, MemberPrincipal principal) {
        GivingSchedule schedule = own(id, principal);
        schedule.setActive(active);
        return GivingScheduleResponse.from(repository.save(schedule));
    }

    public void delete(UUID id, MemberPrincipal principal) {
        repository.delete(own(id, principal));
    }

    private GivingSchedule own(UUID id, MemberPrincipal principal) {
        return repository.findByChurchIdAndMemberIdAndId(principal.getChurchId(), principal.getMemberId(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found"));
    }
}
