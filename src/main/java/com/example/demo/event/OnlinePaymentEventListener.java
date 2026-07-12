package com.example.demo.event;

import com.example.demo.model.Church;
import com.example.demo.model.Member;
import com.example.demo.model.PaystackTransaction;
import com.example.demo.model.Role;
import com.example.demo.repository.ChurchRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PaystackTransactionRepository;
import com.example.demo.service.EmailService;
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
public class OnlinePaymentEventListener {

    private final PaystackTransactionRepository paystackTransactionRepository;
    private final MemberRepository memberRepository;
    private final ChurchRepository churchRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSucceeded(OnlinePaymentSucceededEvent event) {
        PaystackTransaction tx = paystackTransactionRepository.findById(event.getTransactionId()).orElse(null);
        if (tx == null) return;

        Member member = memberRepository.findByChurchIdAndId(tx.getChurchId(), tx.getMemberId()).orElse(null);
        Church church = churchRepository.findById(tx.getChurchId()).orElse(null);
        String churchName = church != null ? church.getChurchName() : "your church";

        // Thank-you notification + email receipt to the giver
        if (member != null) {
            notificationService.notifyMember(member, "Payment Successful 🙏",
                    "Your " + label(tx) + " of GHS " + tx.getAmount() + " was received. Thank you for your faithfulness!");

            if (member.getEmail() != null && !member.getEmail().isBlank()) {
                emailService.sendPaymentReceipt(
                        member.getEmail(), member.getFullName(), churchName,
                        tx.getAmount().toPlainString(), label(tx),
                        tx.getPaystackReference(), tx.getChannel());
            }
        }

        // Let the Financial Secretaries know money came in online
        List<Member> finSecs = memberRepository.findByChurchIdAndRoleIn(
                tx.getChurchId(), List.of(Role.FINANCIAL_SECRETARY));
        if (!finSecs.isEmpty() && member != null) {
            notificationService.notifyMembers(finSecs, "Online Payment Received",
                    member.getFullName() + " paid GHS " + tx.getAmount() + " (" + label(tx) + ") via Paystack.");
        }
    }

    private String label(PaystackTransaction tx) {
        return tx.getPaymentType().name().toLowerCase().replace('_', ' ');
    }
}
