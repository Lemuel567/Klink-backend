package com.example.demo.service;

import com.example.demo.dto.request.AddStoreItemRequest;
import com.example.demo.dto.request.BuyStoreItemRequest;
import com.example.demo.dto.request.UpdateStoreItemRequest;
import com.example.demo.dto.response.StoreItemResponse;
import com.example.demo.dto.response.StorePaymentResponse;
import com.example.demo.model.*;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.StoreItemRepository;
import com.example.demo.repository.StorePaymentRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StoreService {

    private final StoreItemRepository storeItemRepository;
    private final MemberRepository memberRepository;
    private final StorePaymentRepository storePaymentRepository;
    private final SupabaseStorageService storageService;

    /**
     * JSON create flow (2026-07-12): the Manager pre-uploads photos through
     * POST /media/upload and sends their URLs here — supports MULTIPLE pictures
     * of the same item. The first photo becomes the cover (photoUrl).
     */
    public StoreItemResponse addItem(AddStoreItemRequest request, MemberPrincipal principal) {
        RoleChecker.requireManager(principal);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item name is required");
        }
        if (request.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price is required");
        }
        if (request.getQuantity() == null || request.getQuantity() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be zero or more");
        }

        java.util.List<String> photos = request.getPhotoUrls();
        StoreItem item = StoreItem.builder()
                .church(principal.getMember().getChurch())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .category(request.getCategory())
                .photoUrls(photos)
                .photoUrl(photos != null && !photos.isEmpty() ? photos.get(0) : null)
                .status(request.getQuantity() > 0 ? StoreItemStatus.AVAILABLE : StoreItemStatus.SOLD_OUT)
                .createdBy(principal.getMemberId())
                .build();

        return StoreItemResponse.from(storeItemRepository.save(item));
    }

    /** JSON update flow — patch semantics; photoUrls replaces the whole gallery when sent. */
    public StoreItemResponse updateItem(UUID itemId, UpdateStoreItemRequest request, MemberPrincipal principal) {
        RoleChecker.requireManager(principal);

        StoreItem item = storeItemRepository.findByChurchIdAndId(principal.getChurchId(), itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        if (request.getName() != null) item.setName(request.getName());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        if (request.getCategory() != null) item.setCategory(request.getCategory());
        if (request.getPhotoUrls() != null) {
            item.setPhotoUrls(request.getPhotoUrls());
            item.setPhotoUrl(request.getPhotoUrls().isEmpty() ? null : request.getPhotoUrls().get(0));
        }
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
            item.setStatus(request.getQuantity() > 0 ? StoreItemStatus.AVAILABLE : StoreItemStatus.SOLD_OUT);
        }

        return StoreItemResponse.from(storeItemRepository.save(item));
    }

    public StoreItemResponse addItem(AddStoreItemRequest request,
                                      MultipartFile photo,
                                      MemberPrincipal principal) {
        RoleChecker.requireManager(principal);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item name is required");
        }
        if (request.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price is required");
        }
        if (request.getQuantity() == null || request.getQuantity() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be zero or more");
        }

        String photoUrl = null;
        if (photo != null && !photo.isEmpty()) {
            photoUrl = storageService.uploadImage(photo, "store/" + principal.getChurchId());
        }

        StoreItemStatus status = request.getQuantity() > 0
                ? StoreItemStatus.AVAILABLE
                : StoreItemStatus.SOLD_OUT;

        StoreItem item = StoreItem.builder()
                .church(principal.getMember().getChurch())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .category(request.getCategory())
                .photoUrl(photoUrl)
                .status(status)
                .createdBy(principal.getMemberId())
                .build();

        return StoreItemResponse.from(storeItemRepository.save(item));
    }

    public StoreItemResponse updateItem(UUID itemId,
                                         UpdateStoreItemRequest request,
                                         MultipartFile photo,
                                         MemberPrincipal principal) {
        RoleChecker.requireManager(principal);

        StoreItem item = storeItemRepository.findByChurchIdAndId(principal.getChurchId(), itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        if (request.getName() != null) item.setName(request.getName());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        if (request.getCategory() != null) item.setCategory(request.getCategory());

        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
            // Auto-update status based on new quantity
            item.setStatus(request.getQuantity() > 0
                    ? StoreItemStatus.AVAILABLE
                    : StoreItemStatus.SOLD_OUT);
        }

        if (photo != null && !photo.isEmpty()) {
            if (item.getPhotoUrl() != null) {
                storageService.deleteFile(item.getPhotoUrl());
            }
            item.setPhotoUrl(storageService.uploadImage(photo, "store/" + principal.getChurchId()));
        }

        return StoreItemResponse.from(storeItemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public Page<StoreItemResponse> browseStore(MemberPrincipal principal, Pageable pageable) {
        return storeItemRepository.findByChurchId(principal.getChurchId(), pageable)
                .map(StoreItemResponse::from);
    }

    public StorePaymentResponse buyItem(BuyStoreItemRequest request, MemberPrincipal principal) {
        // 2026-07-12: a FINANCIAL_SECRETARY may record an offline/cash sale ON
        // BEHALF OF another member (same church). The payment reference is only
        // mandatory for member self-purchases (MoMo); FinSec cash sales may
        // leave it blank or use a receipt number.
        boolean isFinSec = principal.getRole() == Role.FINANCIAL_SECRETARY;
        Member buyer = principal.getMember();
        if (request.getMemberId() != null
                && !request.getMemberId().equals(principal.getMemberId())
                && isFinSec) {
            buyer = memberRepository.findByChurchIdAndId(principal.getChurchId(), request.getMemberId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        }
        if (!isFinSec && (request.getMomoReference() == null || request.getMomoReference().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile Money reference is required");
        }

        StoreItem item = storeItemRepository.findByChurchIdAndIdForUpdate(
                        principal.getChurchId(), request.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        // Guard on quantity as well as status — if the two ever drift, quantity
        // must never go negative
        if (item.getStatus() == StoreItemStatus.SOLD_OUT || item.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This item is sold out");
        }

        // Decrement quantity and auto-set sold_out if it hits zero
        int newQty = item.getQuantity() - 1;
        item.setQuantity(newQty);
        if (newQty == 0) {
            item.setStatus(StoreItemStatus.SOLD_OUT);
        }
        storeItemRepository.save(item);

        StorePayment payment = StorePayment.builder()
                .church(principal.getMember().getChurch())
                .member(buyer)
                .item(item)
                .amount(item.getPrice())
                .datePaid(request.getDatePaid() != null ? request.getDatePaid() : LocalDate.now())
                .momoReference(request.getMomoReference())
                .collectionStatus(CollectionStatus.AWAITING)
                .build();

        return StorePaymentResponse.from(storePaymentRepository.save(payment));
    }

    public StorePaymentResponse markCollected(UUID paymentId, MemberPrincipal principal) {
        RoleChecker.requireManager(principal);

        StorePayment payment = storePaymentRepository.findByChurchIdAndId(
                        principal.getChurchId(), paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));

        if (payment.getCollectionStatus() == CollectionStatus.COLLECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item already marked as collected");
        }

        payment.setCollectionStatus(CollectionStatus.COLLECTED);
        payment.setCollectedBy(principal.getMemberId());
        payment.setCollectedAt(LocalDateTime.now());

        return StorePaymentResponse.from(storePaymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public Page<StorePaymentResponse> getStoreRevenue(MemberPrincipal principal, Pageable pageable) {
        // Manager included: they alone mark payments collected (markCollected), so they
        // must be able to see the payments list — excluding them broke the collect flow.
        RoleChecker.require(principal, "Access denied",
                Role.FINANCIAL_SECRETARY, Role.PASTOR, Role.ELDER, Role.MANAGER);

        return storePaymentRepository.findByChurchId(principal.getChurchId(), pageable)
                .map(StorePaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<StorePaymentResponse> getMyPurchases(MemberPrincipal principal, Pageable pageable) {
        return storePaymentRepository.findByChurchIdAndMemberId(
                        principal.getChurchId(), principal.getMemberId(), pageable)
                .map(StorePaymentResponse::from);
    }

}
