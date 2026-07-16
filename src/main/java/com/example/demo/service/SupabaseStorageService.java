package com.example.demo.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-key}")
    private String serviceKey;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    private RestClient restClient;

    @PostConstruct
    void init() {
        // 2026-07-12: Supabase's NEW key format (sb_secret_...) is not a JWT —
        // sent only as "Authorization: Bearer" the gateway rejects it with
        // "Invalid Compact JWS" (this silently broke EVERY photo upload). The
        // apikey header authenticates new-format keys and is harmless for
        // legacy JWT keys, so send BOTH. The klink-storage bucket must exist
        // (created 2026-07-12; it had never been created).
        this.restClient = RestClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader("apikey", serviceKey)
                .defaultHeader("Authorization", "Bearer " + serviceKey)
                .build();
    }

    public String uploadImage(MultipartFile file, String folder) {
        validateImageFile(file);
        String path = folder + "/" + UUID.randomUUID() + "." + getExtension(file.getOriginalFilename());
        try {
            restClient.post()
                    .uri("/storage/v1/object/{bucket}/{path}", bucket, path)
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header("x-upsert", "true")
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed");
        }
    }

    public String uploadAudio(MultipartFile file, String folder) {
        validateAudioFile(file);
        String path = folder + "/" + UUID.randomUUID() + "." + getExtension(file.getOriginalFilename());
        try {
            restClient.post()
                    .uri("/storage/v1/object/{bucket}/{path}", bucket, path)
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header("x-upsert", "true")
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Audio upload failed");
        }
    }

    public String uploadPdf(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF files are allowed");
        }
        if (file.getSize() > 30L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds the 30 MB size limit");
        }
        // H2: validate actual file content via magic bytes — Content-Type is attacker-controlled.
        try (InputStream is = file.getInputStream()) {
            byte[] magic = is.readNBytes(4);
            // PDF magic: %PDF (25 50 44 46)
            if (magic.length < 4 || magic[0] != 0x25 || magic[1] != 0x50 || magic[2] != 0x44 || magic[3] != 0x46) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content is not a valid PDF");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read file");
        }
        String path = folder + "/" + UUID.randomUUID() + ".pdf";
        try {
            restClient.post()
                    .uri("/storage/v1/object/{bucket}/{path}", bucket, path)
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("x-upsert", "true")
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
            return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF upload failed");
        }
    }

    public void deleteFile(String publicUrl) {
        if (publicUrl == null) return;
        String path = publicUrl.replace(supabaseUrl + "/storage/v1/object/public/" + bucket + "/", "");
        restClient.delete()
                .uri("/storage/v1/object/{bucket}/{path}", bucket, path)
                .retrieve()
                .toBodilessEntity();
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }
        // H2: validate actual file content via magic bytes — Content-Type is attacker-controlled.
        try (InputStream is = file.getInputStream()) {
            byte[] magic = is.readNBytes(12);
            if (!isValidImageMagic(magic)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content does not match an allowed image format");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read file");
        }
    }

    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("audio/mpeg") &&
                 !contentType.equals("audio/mp4") &&
                 !contentType.equals("audio/x-m4a") &&
                 !contentType.equals("audio/aac"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only audio files are allowed (mp3, m4a, aac)");
        }
        try (InputStream is = file.getInputStream()) {
            byte[] magic = is.readNBytes(8);
            if (!isValidAudioMagic(magic)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "File content does not match an allowed audio format");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read file");
        }
    }

    private boolean isValidAudioMagic(byte[] b) {
        if (b.length < 4) return false;
        // MP3 with ID3 tag: "ID3" header
        if (b[0] == 0x49 && b[1] == 0x44 && b[2] == 0x33) return true;
        // Raw MPEG audio frame sync (MP3): 0xFF followed by sync bits 0xE0-0xFF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xE0) == 0xE0) return true;
        // M4A / AAC in MP4 container: bytes 4-7 are 'ftyp'
        if (b.length >= 8
                && b[4] == 0x66 && b[5] == 0x74 && b[6] == 0x79 && b[7] == 0x70) return true;
        return false;
    }

    private boolean isValidImageMagic(byte[] b) {
        if (b.length < 4) return false;
        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return true;
        // PNG:  89 50 4E 47
        if ((b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47) return true;
        // GIF:  47 49 46 38 (GIF8)
        if (b[0] == 0x47 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x38) return true;
        // WebP: RIFF????WEBP — bytes 0-3 = 52 49 46 46, bytes 8-11 = 57 45 42 50
        if (b.length >= 12
                && b[0] == 0x52 && b[1] == 0x49 && b[2] == 0x46 && b[3] == 0x46
                && b[8] == 0x57 && b[9] == 0x45 && b[10] == 0x42 && b[11] == 0x50) return true;
        // HEIC/HEIF (default iPhone photos): ISO-BMFF 'ftyp' box at offset 4 with an
        // heic/heix/hevc/mif1/msf1 brand. MediaUploadService accepts HEIC, so this
        // second gate must too — without this branch every iPhone photo 400'd here.
        if (b.length >= 12
                && b[4] == 0x66 && b[5] == 0x74 && b[6] == 0x79 && b[7] == 0x70) {
            String brand = new String(b, 8, 4, StandardCharsets.US_ASCII);
            return brand.startsWith("hei") || brand.startsWith("hev")
                    || brand.startsWith("mif1") || brand.startsWith("msf1");
        }
        return false;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
