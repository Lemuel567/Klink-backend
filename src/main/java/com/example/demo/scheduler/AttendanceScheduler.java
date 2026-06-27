package com.example.demo.scheduler;

import com.example.demo.model.*;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.AttendanceSessionRepository;
import com.example.demo.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final MemberRepository memberRepository;

    // Runs every 30 minutes from 6am to 11:30pm — no need to poll overnight when no services run
    @Scheduled(cron = "0 0/30 6-23 * * *")
    @Transactional
    public void markAbsentMembers() {
        List<AttendanceSession> expiredSessions = attendanceSessionRepository
                .findByProcessedFalseAndExpiresAtBefore(LocalDateTime.now());

        for (AttendanceSession session : expiredSessions) {
            // Single bulk query replaces the N+1 loop that previously called
            // existsByMemberIdAndServiceDateAndServiceName() once per active member.
            List<Member> absentees = memberRepository.findMembersNotCheckedIn(
                    session.getChurch().getId(),
                    MemberStatus.ACTIVE,
                    session.getServiceDate(),
                    session.getServiceName());

            for (Member member : absentees) {
                Attendance absent = Attendance.builder()
                        .church(session.getChurch())
                        .member(member)
                        .serviceName(session.getServiceName())
                        .serviceDate(session.getServiceDate())
                        .method(AttendanceMethod.MANUAL)
                        .status(AttendanceStatus.ABSENT)
                        .build();
                attendanceRepository.save(absent);
            }

            session.setProcessed(true);
            attendanceSessionRepository.save(session);
        }
    }
}
