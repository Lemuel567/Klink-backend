package com.example.demo.service;

import com.example.demo.dto.request.ManualAttendanceRequest;
import com.example.demo.dto.request.ScanAttendanceRequest;
import com.example.demo.dto.response.AttendanceResponse;
import com.example.demo.dto.response.GenerateAttendanceQrResponse;
import com.example.demo.model.*;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.AttendanceSessionRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.security.JwtUtil;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    public GenerateAttendanceQrResponse generateQr(MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        LocalDate today = LocalDate.now();
        String serviceName = resolveServiceName(today.getDayOfWeek());
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(8);

        AttendanceSession session = AttendanceSession.builder()
                .church(principal.getMember().getChurch())
                .serviceName(serviceName)
                .serviceDate(today)
                .expiresAt(expiresAt)
                .processed(false)
                .build();
        attendanceSessionRepository.save(session);

        String qrPayload = jwtUtil.generateAttendanceToken(principal.getChurchId(), serviceName, today);

        return GenerateAttendanceQrResponse.builder()
                .qrPayload(qrPayload)
                .serviceName(serviceName)
                .serviceDate(today)
                .build();
    }

    public AttendanceResponse scan(ScanAttendanceRequest request, MemberPrincipal principal) {
        String qrPayload = request.getQrPayload();

        if (!jwtUtil.isAttendanceToken(qrPayload)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid QR code");
        }

        if (!jwtUtil.extractAttendanceChurchId(qrPayload).equals(principal.getChurchId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QR code does not belong to your church");
        }

        String serviceName = jwtUtil.extractAttendanceServiceName(qrPayload);
        LocalDate serviceDate = jwtUtil.extractAttendanceServiceDate(qrPayload);

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), principal.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        if (attendanceRepository.existsByMemberIdAndServiceDateAndServiceName(
                principal.getMemberId(), serviceDate, serviceName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance already recorded");
        }

        Attendance attendance = Attendance.builder()
                .church(member.getChurch())
                .member(member)
                .serviceName(serviceName)
                .serviceDate(serviceDate)
                .timeOfScan(LocalDateTime.now())
                .markedBy(principal.getMemberId())
                .method(AttendanceMethod.QR_SCAN)
                .status(AttendanceStatus.PRESENT)
                .build();

        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    public AttendanceResponse markManual(ManualAttendanceRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        Member member = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getMemberId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member is deactivated");
        }

        if (attendanceRepository.existsByMemberIdAndServiceDateAndServiceName(
                request.getMemberId(), request.getServiceDate(), request.getServiceName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance already recorded");
        }

        Attendance attendance = Attendance.builder()
                .church(member.getChurch())
                .member(member)
                .serviceName(request.getServiceName())
                .serviceDate(request.getServiceDate())
                .timeOfScan(LocalDateTime.now())
                .markedBy(principal.getMemberId())
                .method(AttendanceMethod.MANUAL)
                .status(AttendanceStatus.PRESENT)
                .build();

        return AttendanceResponse.from(attendanceRepository.save(attendance));
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getAllAttendance(MemberPrincipal principal, Pageable pageable) {
        RoleChecker.requirePastorElderOrManager(principal);

        return attendanceRepository.findByChurchId(principal.getChurchId(), pageable)
                .map(AttendanceResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getMyAttendance(MemberPrincipal principal, Pageable pageable) {
        return attendanceRepository.findByChurchIdAndMemberId(
                        principal.getChurchId(), principal.getMemberId(), pageable)
                .map(AttendanceResponse::from);
    }

    private String resolveServiceName(DayOfWeek day) {
        return switch (day) {
            case SUNDAY -> "Sunday Service";
            case WEDNESDAY -> "Wednesday Service";
            case FRIDAY -> "Friday Service";
            case SATURDAY -> "Saturday Service";
            default -> day.name().charAt(0) + day.name().substring(1).toLowerCase() + " Service";
        };
    }
}
