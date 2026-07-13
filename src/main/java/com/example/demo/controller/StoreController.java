package com.example.demo.controller;

import com.example.demo.dto.request.AddStoreItemRequest;
import com.example.demo.dto.request.BuyStoreItemRequest;
import com.example.demo.dto.request.UpdateStoreItemRequest;
import com.example.demo.dto.response.StoreItemResponse;
import com.example.demo.dto.response.StorePaymentResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // JSON create/update (2026-07-12): photos are pre-uploaded via POST /media/upload,
    // their URLs sent in photoUrls — supports multiple pictures per item.
    @PostMapping(value = "/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StoreItemResponse> addItemJson(
            @org.springframework.web.bind.annotation.RequestBody AddStoreItemRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storeService.addItem(request, principal));
    }

    @PutMapping(value = "/items/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StoreItemResponse> updateItemJson(
            @PathVariable UUID id,
            @org.springframework.web.bind.annotation.RequestBody UpdateStoreItemRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(storeService.updateItem(id, request, principal));
    }

    @PostMapping(value = "/items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoreItemResponse> addItem(
            @RequestPart("name") String name,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("price") String price,
            @RequestPart("quantity") String quantity,
            @RequestPart(value = "category", required = false) String category,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

        AddStoreItemRequest request = new AddStoreItemRequest();
        request.setName(name);
        request.setDescription(description);
        try {
            request.setPrice(new BigDecimal(price));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be a valid decimal number");
        }
        try {
            request.setQuantity(Integer.parseInt(quantity));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be a valid integer");
        }
        request.setCategory(category);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storeService.addItem(request, photo, principal));
    }

    @PutMapping(value = "/items/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoreItemResponse> updateItem(
            @PathVariable UUID id,
            @RequestPart(value = "name", required = false) String name,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "price", required = false) String price,
            @RequestPart(value = "quantity", required = false) String quantity,
            @RequestPart(value = "category", required = false) String category,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();

        UpdateStoreItemRequest request = new UpdateStoreItemRequest();
        if (name != null) request.setName(name);
        if (description != null) request.setDescription(description);
        if (price != null) {
            try {
                request.setPrice(new BigDecimal(price));
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be a valid decimal number");
            }
        }
        if (quantity != null) {
            try {
                request.setQuantity(Integer.parseInt(quantity));
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be a valid integer");
            }
        }
        if (category != null) request.setCategory(category);

        return ResponseEntity.ok(storeService.updateItem(id, request, photo, principal));
    }

    @GetMapping("/items")
    public ResponseEntity<Page<StoreItemResponse>> browseStore(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(storeService.browseStore(principal, pageable));
    }

    @PostMapping("/pay")
    public ResponseEntity<StorePaymentResponse> buyItem(
            @Valid @RequestBody BuyStoreItemRequest request,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storeService.buyItem(request, principal));
    }

    @PutMapping("/payments/{id}/collect")
    public ResponseEntity<StorePaymentResponse> markCollected(
            @PathVariable UUID id,
            Authentication authentication) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(storeService.markCollected(id, principal));
    }

    @GetMapping("/payments")
    public ResponseEntity<Page<StorePaymentResponse>> getStoreRevenue(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(storeService.getStoreRevenue(principal, pageable));
    }

    @GetMapping("/my-purchases")
    public ResponseEntity<Page<StorePaymentResponse>> getMyPurchases(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(storeService.getMyPurchases(principal, pageable));
    }
}
