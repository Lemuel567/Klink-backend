package com.example.demo.security;

import com.example.demo.model.Member;
import com.example.demo.model.MemberStatus;
import com.example.demo.model.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class MemberPrincipal implements UserDetails {

    private final Member member;
    private final UUID churchId;

    public MemberPrincipal(Member member, UUID churchId) {
        this.member = member;
        this.churchId = churchId;
    }

    public UUID getMemberId() {
        return member.getId();
    }

    public Role getRole() {
        return member.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()));
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return member.getLockedUntil() == null
                || !member.getLockedUntil().isAfter(java.time.LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return member.getStatus() == MemberStatus.ACTIVE;
    }
}
