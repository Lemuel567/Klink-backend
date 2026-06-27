package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LOOPBACK = Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String ip = extractClientIp(request);

        // Localhost bypass applies to IP rate limiting only.
        // Account lockout in AuthService.login() is never bypassed — it runs for every caller.
        if (properties.isBypassLocalhost() && LOOPBACK.contains(ip)) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        boolean limited = false;
        long retryAfterSeconds = 0;

        switch (path) {
            case "/api/v1/auth/register", "/api/v1/auth/register-church" -> {
                limited = rateLimiterService.checkRegister(ip);
                retryAfterSeconds = rateLimiterService.registerRetryAfterSeconds();
            }
            case "/api/v1/auth/forgot-password" -> {
                limited = rateLimiterService.checkForgotPassword(ip);
                retryAfterSeconds = rateLimiterService.forgotPasswordRetryAfterSeconds();
            }
            case "/api/v1/auth/resend-verification" -> {
                limited = rateLimiterService.checkResendVerification(ip);
                retryAfterSeconds = rateLimiterService.resendVerifRetryAfterSeconds();
            }
            case "/api/v1/auth/resend-phone-verification" -> {
                limited = rateLimiterService.checkResendVerification(ip);
                retryAfterSeconds = rateLimiterService.resendVerifRetryAfterSeconds();
            }
            case "/api/v1/auth/refresh" -> {
                limited = rateLimiterService.checkRefresh(ip);
                retryAfterSeconds = rateLimiterService.refreshRetryAfterSeconds();
            }
            // /api/v1/auth/login is intentionally absent here.
            // Login rate limiting uses email+IP key and is enforced in AuthService.login()
            // where the parsed email from @RequestBody is already available — reading it
            // here in a filter would consume the request body stream and break @RequestBody.
        }

        if (limited) {
            reject(response, retryAfterSeconds);
            return;
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write("{\"error\":\"Too many requests. Try again in "
                + (retryAfterSeconds / 60) + " minutes.\"}");
    }

    private String extractClientIp(HttpServletRequest request) {
        // X-Forwarded-For is attacker-controlled and not trusted — use only the TCP-level remote address.
        // Configure the upstream proxy/load balancer to set REMOTE_ADDR to the real client IP instead.
        return request.getRemoteAddr();
    }
}
