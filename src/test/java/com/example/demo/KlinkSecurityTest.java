package com.example.demo;
//Tests for member authentication ,store access and payment security rules

import com.example.demo.dto.request.AssignRoleRequest;
import com.example.demo.dto.request.BuyStoreItemRequest;
import com.example.demo.dto.request.UpdateMemberRequest;
import com.example.demo.model.*;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.StoreItemRepository;
import com.example.demo.repository.StorePaymentRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.MemberService;
import com.example.demo.service.StoreService;
import com.example.demo.service.SupabaseStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KlinkSecurityTest {

    @Mock MemberRepository memberRepository;
    @Mock StoreItemRepository storeItemRepository;
    @Mock StorePaymentRepository storePaymentRepository;
    @Mock SupabaseStorageService storageService;

    @InjectMocks MemberService memberService;
    @InjectMocks StoreService storeService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private MemberPrincipal principalOf(UUID churchId, Role role) {
        Church church = Church.builder().id(churchId).churchName("Test Church").build();
        Member member = Member.builder()
                .id(UUID.randomUUID())
                .church(church)
                .role(role)
                .status(MemberStatus.ACTIVE)
                .fullName("Test " + role)
                .hasSmartphone(true)
                .build();
        return new MemberPrincipal(member, churchId);
    }

    private StoreItem itemWithQty(UUID churchId, int qty) {
        Church church = Church.builder().id(churchId).churchName("Test Church").build();
        return StoreItem.builder()
                .id(UUID.randomUUID())
                .church(church)
                .name("Test Item")
                .price(new BigDecimal("10.00"))
                .quantity(qty)
                .status(qty > 0 ? StoreItemStatus.AVAILABLE : StoreItemStatus.SOLD_OUT)
                .build();
    }

    // ── 1. Church isolation ───────────────────────────────────────────────────

    /**
     * getAllMembers must query the repository using the churchId from the
     * principal (JWT), never a churchId from any other source.
     */
    @Test
    void getAllMembers_queriesOnlyCallerChurch() {
        UUID churchA = UUID.randomUUID();
        UUID churchB = UUID.randomUUID();
        MemberPrincipal principal = principalOf(churchA, Role.PASTOR);

        when(memberRepository.findByChurchId(eq(churchA), any(Pageable.class)))
                .thenReturn(Page.empty());

        memberService.getAllMembers(principal, null, Pageable.ofSize(20));

        verify(memberRepository).findByChurchId(eq(churchA), any(Pageable.class));
        verify(memberRepository, never()).findByChurchId(eq(churchB), any(Pageable.class));
    }

    /**
     * Looking up a member that exists in Church B while authenticated as Church A
     * must return 404, not the cross-church data.
     */
    @Test
    void updateMember_crossChurchAccessReturns404() {
        UUID churchA = UUID.randomUUID();
        UUID memberIdInChurchB = UUID.randomUUID();
        MemberPrincipal principal = principalOf(churchA, Role.PASTOR);

        // Repository returns empty because the member belongs to Church B, not A
        when(memberRepository.findByChurchIdAndId(churchA, memberIdInChurchB))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.updateMember(memberIdInChurchB, new UpdateMemberRequest(), principal))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // ── 2. Role assignment rules ──────────────────────────────────────────────

    /**
     * A Pastor calling assignRole with newRole=PASTOR must be rejected (403).
     * Only an Elder can appoint a new Pastor.
     */
    @Test
    void assignRole_pastorCannotAppointAnotherPastor() {
        UUID churchId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        MemberPrincipal pastor = principalOf(churchId, Role.PASTOR);

        Member target = Member.builder()
                .id(targetId).role(Role.MEMBER).status(MemberStatus.ACTIVE)
                .fullName("Target").hasSmartphone(true)
                .church(pastor.getMember().getChurch()).build();

        when(memberRepository.findByChurchIdAndId(churchId, targetId))
                .thenReturn(Optional.of(target));

        AssignRoleRequest request = new AssignRoleRequest();
        ReflectionTestUtils.setField(request, "role", Role.PASTOR);

        assertThatThrownBy(() -> memberService.assignRole(targetId, request, pastor))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(403));
    }

    /**
     * An Elder calling assignRole with newRole=ELDER must be rejected (403).
     * Only the Pastor can appoint Elders.
     */
    @Test
    void assignRole_elderCannotAppointAnotherElder() {
        UUID churchId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        MemberPrincipal elder = principalOf(churchId, Role.ELDER);

        Member target = Member.builder()
                .id(targetId).role(Role.MEMBER).status(MemberStatus.ACTIVE)
                .fullName("Target").hasSmartphone(true)
                .church(elder.getMember().getChurch()).build();

        when(memberRepository.findByChurchIdAndId(churchId, targetId))
                .thenReturn(Optional.of(target));

        AssignRoleRequest request = new AssignRoleRequest();
        ReflectionTestUtils.setField(request, "role", Role.ELDER);

        assertThatThrownBy(() -> memberService.assignRole(targetId, request, elder))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(403));
    }

    /**
     * An Elder cannot revoke Elder privileges from any Elder, including themselves.
     */
    @Test
    void assignRole_elderCannotDemoteElder() {
        UUID churchId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        MemberPrincipal elder = principalOf(churchId, Role.ELDER);

        Member targetElder = Member.builder()
                .id(targetId).role(Role.ELDER).status(MemberStatus.ACTIVE)
                .fullName("Other Elder").hasSmartphone(true)
                .church(elder.getMember().getChurch()).build();

        when(memberRepository.findByChurchIdAndId(churchId, targetId))
                .thenReturn(Optional.of(targetElder));

        AssignRoleRequest request = new AssignRoleRequest();
        ReflectionTestUtils.setField(request, "role", Role.MEMBER);

        assertThatThrownBy(() -> memberService.assignRole(targetId, request, elder))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(403));
    }

    // ── 3. Store purchase — sold-out protection ───────────────────────────────

    /**
     * Purchasing the final unit must decrement the inventory to zero and mark
     * the item as SOLD_OUT, preventing any subsequent buyers from completing
     * the purchase.
     */
    @Test
    void buyItem_lastItemSetsStatusToSoldOut() {
        UUID churchId = UUID.randomUUID();
        MemberPrincipal member = principalOf(churchId, Role.MEMBER);

        StoreItem item = itemWithQty(churchId, 1);
        UUID itemId = item.getId();

        when(storeItemRepository.findByChurchIdAndIdForUpdate(churchId, itemId))
                .thenReturn(Optional.of(item));
        when(storeItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(storePaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BuyStoreItemRequest req = new BuyStoreItemRequest();
        ReflectionTestUtils.setField(req, "itemId", itemId);
        ReflectionTestUtils.setField(req, "momoReference", "MOMO-TEST-123");

        storeService.buyItem(req, member);

        assertThat(item.getQuantity()).isZero();
        assertThat(item.getStatus()).isEqualTo(StoreItemStatus.SOLD_OUT);
    }

    /**
     * Purchasing an item after its inventory has been exhausted must return
     * HTTP 409 Conflict. This is the expected outcome for the second concurrent
     * buyer after the first buyer successfully purchases the last available unit.
     */
    @Test
    void buyItem_soldOutItemThrowsConflict() {
        UUID churchId = UUID.randomUUID();
        MemberPrincipal member = principalOf(churchId, Role.MEMBER);

        StoreItem item = itemWithQty(churchId, 0);
        UUID itemId = item.getId();

        when(storeItemRepository.findByChurchIdAndIdForUpdate(churchId, itemId))
                .thenReturn(Optional.of(item));

        BuyStoreItemRequest req = new BuyStoreItemRequest();
        ReflectionTestUtils.setField(req, "itemId", itemId);
        ReflectionTestUtils.setField(req, "momoReference", "MOMO-TEST-123");

        assertThatThrownBy(() -> storeService.buyItem(req, member))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(409));
    }
}
