package com.example.demo.security;

import com.example.demo.model.Member;
import com.example.demo.model.MemberStatus;
import com.example.demo.model.Role;
import com.example.demo.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            rejectUnauthorized(response);
            return;
        }

        // Attendance QR tokens are signed with the same key but must never authenticate HTTP requests.
        // Without this check, a QR token passed as Bearer would reach extractMemberId() which returns
        // null (no memberId claim), and UUID.fromString(null) throws IllegalArgumentException → 500.
        if (jwtUtil.isAttendanceToken(token)) {
            rejectUnauthorized(response);
            return;
        }

        UUID memberId = jwtUtil.extractMemberId(token);
        UUID churchId = jwtUtil.extractChurchId(token);

        // JOIN FETCH church so we can check deletedAt without a lazy-load outside a transaction
        Member member = memberRepository.findByIdWithChurch(memberId).orElse(null);

        if (member == null) {
            rejectUnauthorized(response);
            return;
        }

        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            rejectUnauthorized(response);
            return;
        }

        boolean isVerified = Boolean.TRUE.equals(member.getEmailVerified())
                || Boolean.TRUE.equals(member.getPhoneVerified());
        if (!isVerified) {
            rejectNotVerified(response);
            return;
        }

        if (member.getPasswordChangedAt() != null) {
            LocalDateTime issuedAt = jwtUtil.extractIssuedAt(token);
            if (issuedAt.isBefore(member.getPasswordChangedAt())) {
                rejectUnauthorized(response);
                return;
            }
        }

        if (jwtUtil.extractTokenVersion(token) != member.getTokenVersion()) {
            rejectUnauthorized(response);
            return;
        }

        // Block all requests when the church is soft-deleted, EXCEPT an Elder calling the restore endpoint
        if (member.getChurch().getDeletedAt() != null) {
            boolean isRestoreEndpoint = "POST".equals(request.getMethod())
                    && request.getRequestURI().endsWith("/api/v1/church/restore");
            if (!isRestoreEndpoint || member.getRole() != Role.ELDER) {
                rejectChurchDeleted(response, member.getChurch().getDeletedAt().plusDays(30));
                return;
            }
        }

        MemberPrincipal principal = new MemberPrincipal(member, churchId);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private void rejectUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Unauthorized\"}");
    }

    private void rejectNotVerified(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Account not verified. Please verify your email or phone number before accessing this resource.\"}");
    }

    private void rejectChurchDeleted(HttpServletResponse response, LocalDateTime scheduledDeletionAt) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"This church has been scheduled for deletion on "
                + scheduledDeletionAt.toLocalDate()
                + ". Contact an Elder to restore it.\"}");
    }
}
