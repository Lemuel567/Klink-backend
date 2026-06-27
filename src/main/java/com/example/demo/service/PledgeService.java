package com.example.demo.service;

import com.example.demo.dto.request.RecordPledgePaymentRequest;
import com.example.demo.dto.request.RecordPledgeRequest;
import com.example.demo.dto.response.PledgePaymentResponse;
import com.example.demo.dto.response.PledgeResponse;
import com.example.demo.model.*;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PledgePaymentRepository;
import com.example.demo.repository.PledgeRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PledgeService {

    private final PledgeRepository pledgeRepository;
    private final PledgePaymentRepository pledgePaymentRepository;
    private final MemberRepository memberRepository;

    // Financial Secretary records a pledge on behalf of a member
    public PledgeResponse recordPledge(RecordPledgeRequest request, MemberPrincipal principal) {
        RoleChecker.requireFinancialSecretary(principal);

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        Pledge pledge = Pledge.builder()
                .church(principal.getMember().getChurch())
                .member(member)
                .description(request.getDescription())
                .amount(request.getAmount())
                .amountPaid(BigDecimal.ZERO)
                .status(PledgeStatus.UNPAID)
                .recordedBy(principal.getMemberId())
                .build();

        return PledgeResponse.from(pledgeRepository.save(pledge));
    }

    public PledgePaymentResponse payPledge(UUID pledgeId, RecordPledgePaymentRequest request, MemberPrincipal principal) {
        RoleChecker.requireFinancialSecretary(principal);

        Pledge pledge = pledgeRepository.findByChurchIdAndId(principal.getChurchId(), pledgeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pledge not found"));

        if (pledge.getStatus() == PledgeStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This pledge has already been fully paid");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero");
        }

        PledgePayment payment = PledgePayment.builder()
                .church(principal.getMember().getChurch())
                .pledge(pledge)
                .member(pledge.getMember())
                .amount(request.getAmount())
                .paymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now())
                .recordedBy(principal.getMemberId())
                .build();

        pledgePaymentRepository.save(payment);

        // Update running total on the pledge
        BigDecimal currentPaid = pledge.getAmountPaid() != null ? pledge.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal newAmountPaid = currentPaid.add(request.getAmount());
        pledge.setAmountPaid(newAmountPaid);

        if (newAmountPaid.compareTo(pledge.getAmount()) >= 0) {
            pledge.setStatus(PledgeStatus.PAID);
            pledge.setPaidAt(payment.getPaymentDate());
        } else {
            pledge.setStatus(PledgeStatus.PARTIALLY_PAID);
        }

        pledgeRepository.save(pledge);

        return PledgePaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public Page<PledgeResponse> getAllPledges(MemberPrincipal principal, Pageable pageable) {
        RoleChecker.requireFinancialSecretaryOrPrivileged(principal);

        return pledgeRepository.findByChurchId(principal.getChurchId(), pageable)
                .map(PledgeResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<PledgeResponse> getMyPledges(MemberPrincipal principal, Pageable pageable) {
        return pledgeRepository.findByChurchIdAndMemberId(
                        principal.getChurchId(), principal.getMemberId(), pageable)
                .map(PledgeResponse::from);
    }

    @Transactional(readOnly = true)
    public List<PledgePaymentResponse> getPledgePayments(UUID pledgeId, MemberPrincipal principal) {
        Pledge pledge = pledgeRepository.findByChurchIdAndId(principal.getChurchId(), pledgeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pledge not found"));

        Role role = principal.getRole();
        boolean isOwner = pledge.getMember().getId().equals(principal.getMemberId());
        boolean isPrivileged = role == Role.FINANCIAL_SECRETARY || role == Role.PASTOR || role == Role.ELDER;

        if (!isOwner && !isPrivileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return pledgePaymentRepository.findByChurchIdAndPledgeId(principal.getChurchId(), pledgeId)
                .stream()
                .map(PledgePaymentResponse::from)
                .toList();
    }

}
