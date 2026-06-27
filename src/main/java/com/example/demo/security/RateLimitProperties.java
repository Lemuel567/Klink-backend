package com.example.demo.security;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("security.rate-limit")
public class RateLimitProperties {

    private boolean bypassLocalhost = true;
    private Login    login              = new Login();
    private Endpoint register           = new Endpoint();
    private Endpoint resendVerification = new Endpoint();
    private Endpoint forgotPassword     = new Endpoint();
    private Endpoint refresh            = new Endpoint();
    private Endpoint verifyEmail        = new Endpoint();
    private Endpoint resetPassword      = new Endpoint();
    private Endpoint phoneVerify        = new Endpoint();
    private Endpoint phoneResend        = new Endpoint();
    private Endpoint phoneUpdate        = new Endpoint();

    @Data
    @NoArgsConstructor
    public static class Login {
        private int maxAttemptsPerAccount = 5;   // email+IP combined key
        private int maxAttemptsPerIp      = 20;  // IP-only key — higher to allow shared WiFi
        private int windowMinutes         = 15;
        private int failureResetHours     = 24;  // counter resets after this many hours of inactivity
        private int lockoutAttempts       = 5;   // DB-level account lockout threshold
        private int lockoutMinutes        = 15;  // how long the DB-level lockout lasts

        public long windowMillis() {
            return windowMinutes * 60_000L;
        }
    }

    @Data
    @NoArgsConstructor
    public static class Endpoint {
        private int maxAttempts   = 5;
        private int windowMinutes = 0;
        private int windowHours   = 0;

        public long windowMillis() {
            if (windowHours > 0)   return windowHours * 3_600_000L;
            if (windowMinutes > 0) return windowMinutes * 60_000L;
            return 3_600_000L;
        }
    }
}
