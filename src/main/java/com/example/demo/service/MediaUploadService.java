package com.example.demo.service;

import com.example.demo.dto.response.MediaUploadResponse;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MediaUploadService {

    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif"
    );

    private final SupabaseStorageService storageService;

    public MediaUploadResponse upload(MultipartFile file, String folder, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds 10 MB limit");
        }

        String mimeType = detectMimeType(file);
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only JPEG, PNG, WebP, and HEIC images are accepted");
        }

        String targetFolder = sanitizeFolder(folder);
        String url = storageService.uploadImage(file, targetFolder);

        return MediaUploadResponse.builder()
                .imageUrl(url)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(mimeType)
                .build();
    }

    private String detectMimeType(MultipartFile file) {
        try {
            byte[] header = file.getBytes();
            if (header.length < 4) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too small to determine type");
            }

            // JPEG: FF D8 FF
            if (matchesPrefix(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
                return "image/jpeg";
            }
            // PNG: 89 50 4E 47
            if (matchesPrefix(header, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47})) {
                return "image/png";
            }
            // WebP: RIFF????WEBP
            if (matchesPrefix(header, new byte[]{0x52, 0x49, 0x46, 0x46}) && header.length >= 12
                    && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
                return "image/webp";
            }
            // HEIC/HEIF: ftyp box at offset 4 with brand heic/heix/hevc/mif1
            if (header.length >= 12) {
                String brand = new String(header, 4, 4);
                if (brand.startsWith("ftyp")) {
                    String subBrand = header.length >= 16 ? new String(header, 8, 4) : "";
                    if (subBrand.startsWith("heic") || subBrand.startsWith("heix")
                            || subBrand.startsWith("mif1") || subBrand.startsWith("msf1")) {
                        return "image/heic";
                    }
                }
            }

            // Fall back to declared content type (less trusted)
            String declared = file.getContentType();
            if (declared != null && ALLOWED_MIME_TYPES.contains(declared)) {
                return declared;
            }

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File type not recognised or not allowed");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file");
        }
    }

    private boolean matchesPrefix(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) return "uploads";
        // Strip path traversal attempts
        return folder.replaceAll("[^a-zA-Z0-9_\\-]", "");
    }
}