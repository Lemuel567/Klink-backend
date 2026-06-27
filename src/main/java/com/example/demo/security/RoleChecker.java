package com.example.demo.security;

import com.example.demo.model.Role;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class RoleChecker {

    private RoleChecker() {}

    public static void require(MemberPrincipal principal, String message, Role... roles) {
        Role actual = principal.getRole();
        for (Role r : roles) {
            if (actual == r) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    public static void requirePastorOrElder(MemberPrincipal principal) {
        require(principal, "Only a Pastor or Elder can perform this action",
                Role.PASTOR, Role.ELDER);
    }

    public static void requirePastorElderOrManager(MemberPrincipal principal) {
        require(principal, "Access denied",
                Role.PASTOR, Role.ELDER, Role.MANAGER);
    }

    public static void requireManager(MemberPrincipal principal) {
        require(principal, "Only a Manager can perform this action",
                Role.MANAGER);
    }

    public static void requireFinancialSecretary(MemberPrincipal principal) {
        require(principal, "Only the Financial Secretary can perform this action",
                Role.FINANCIAL_SECRETARY);
    }

    public static void requireFinancialSecretaryOrPrivileged(MemberPrincipal principal) {
        require(principal, "Access denied",
                Role.FINANCIAL_SECRETARY, Role.PASTOR, Role.ELDER);
    }

    public static void requirePastorOrManager(MemberPrincipal principal) {
        require(principal, "Only a Pastor or Manager can perform this action",
                Role.PASTOR, Role.MANAGER);
    }

    public static boolean isPastorOrElder(MemberPrincipal principal) {
        Role r = principal.getRole();
        return r == Role.PASTOR || r == Role.ELDER;
    }
}
