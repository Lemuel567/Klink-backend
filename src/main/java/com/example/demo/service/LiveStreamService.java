package com.example.demo.service;

import com.example.demo.dto.request.StartLiveStreamRequest;
import com.example.demo.dto.response.LiveStreamResponse;
import com.example.demo.event.NotificationEvent;
import com.example.demo.model.LiveStream;
import com.example.demo.model.LiveStreamProvider;
import com.example.demo.model.LiveStreamStatus;
import com.example.demo.repository.LiveStreamRepository;
import com.example.demo.security.MemberPrincipal;
import com.example.demo.security.RoleChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tier-1 live streaming. The church broadcasts on YouTube Live; Klink stores
 * only the video id so members can watch it embedded inside the app, and pushes
 * a "we're live" notification to the congregation.
 *
 * No media data ever passes through this backend — YouTube handles ingest,
 * transcoding, adaptive bitrate and CDN delivery.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LiveStreamService {

    private final LiveStreamRepository liveStreamRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * A stream still marked LIVE after this long is treated as forgotten, not
     * actually live. Without this, one leader forgetting to tap "End" would pin
     * a "🔴 Live now" badge to every member's home screen indefinitely.
     */
    private static final Duration MAX_LIVE_DURATION = Duration.ofHours(12);

    /** A YouTube video id is exactly 11 chars of [A-Za-z0-9_-]. */
    private static final Pattern BARE_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    /** Ordered: the ?v= form is by far the most common, so it is tried first. */
    private static final List<Pattern> URL_PATTERNS = List.of(
            Pattern.compile("[?&]v=([A-Za-z0-9_-]{11})"),
            Pattern.compile("youtu\\.be/([A-Za-z0-9_-]{11})"),
            Pattern.compile("youtube\\.com/live/([A-Za-z0-9_-]{11})"),
            Pattern.compile("youtube\\.com/embed/([A-Za-z0-9_-]{11})"),
            Pattern.compile("youtube\\.com/shorts/([A-Za-z0-9_-]{11})")
    );

    /** Hosts whose links we accept as Facebook video/live posts. */
    private static final Pattern FACEBOOK_HOST = Pattern.compile(
            "^https?://(www\\.|web\\.|m\\.|business\\.)?(facebook\\.com|fb\\.watch)/", Pattern.CASE_INSENSITIVE);

    /** A parsed link: which platform, and the handle the embed needs. */
    public record ParsedSource(LiveStreamProvider provider, String sourceRef) {}

    /**
     * Works out which platform a pasted link belongs to and extracts the handle
     * the player needs. Detection is automatic so a leader just pastes whatever
     * their platform gave them — they never pick a provider from a menu.
     */
    public static ParsedSource parseSource(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A stream link is required");
        }

        if (FACEBOOK_HOST.matcher(url).find()) {
            // Facebook's embed takes the full post URL as an href, so keep it whole.
            if (url.length() > 600) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That Facebook link is too long");
            }
            return new ParsedSource(LiveStreamProvider.FACEBOOK, url);
        }

        // Anything else is treated as YouTube (covers bare 11-char ids too).
        return new ParsedSource(LiveStreamProvider.YOUTUBE, extractYoutubeVideoId(url));
    }

    /**
     * Pulls the video id out of any YouTube link a leader might paste. Parsing
     * lives on the server so there is a single source of truth and a malformed
     * link can never reach the database.
     */
    public static String extractYoutubeVideoId(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A YouTube link is required");
        }
        for (Pattern pattern : URL_PATTERNS) {
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        if (BARE_ID.matcher(url).matches()) {
            return url;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "That doesn't look like a YouTube or Facebook link. Copy the link from YouTube "
                        + "(e.g. https://youtu.be/abc123XYZ_0) or from your Facebook live video post.");
    }

    public LiveStreamResponse startStream(StartLiveStreamRequest request, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        ParsedSource source = parseSource(request.getStreamUrl());
        UUID churchId = principal.getChurchId();
        LocalDateTime now = LocalDateTime.now();

        // A leader who forgot to end last week's stream must not be blocked from
        // going live today — close anything still open for this church first.
        List<LiveStream> stillOpen = liveStreamRepository.findByChurchIdAndStatus(churchId, LiveStreamStatus.LIVE);
        if (!stillOpen.isEmpty()) {
            for (LiveStream open : stillOpen) {
                open.setStatus(LiveStreamStatus.ENDED);
                open.setEndedAt(now);
            }
            liveStreamRepository.saveAll(stillOpen);
        }

        LiveStream stream = LiveStream.builder()
                .church(principal.getMember().getChurch())
                .title(request.getTitle().trim())
                .provider(source.provider())
                .sourceRef(source.sourceRef())
                .status(LiveStreamStatus.LIVE)
                .startedBy(principal.getMemberId())
                .startedAt(now)
                .build();

        LiveStream saved = liveStreamRepository.save(stream);

        // Fans out to the whole church AFTER_COMMIT (async) — never inside the tx.
        eventPublisher.publishEvent(new NotificationEvent(
                this, churchId, "🔴 Live now", saved.getTitle()));

        return LiveStreamResponse.from(saved);
    }

    public LiveStreamResponse endStream(UUID id, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);

        LiveStream stream = liveStreamRepository.findByChurchIdAndId(principal.getChurchId(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live stream not found"));

        // Idempotent: ending an already-ended stream is a no-op, not an error.
        if (stream.getStatus() == LiveStreamStatus.LIVE) {
            stream.setStatus(LiveStreamStatus.ENDED);
            stream.setEndedAt(LocalDateTime.now());
            liveStreamRepository.save(stream);
        }
        return LiveStreamResponse.from(stream);
    }

    /** The church's current broadcast, or null when nothing is live. */
    @Transactional(readOnly = true)
    public LiveStreamResponse getCurrentLive(MemberPrincipal principal) {
        return liveStreamRepository
                .findFirstByChurchIdAndStatusAndStartedAtAfterOrderByStartedAtDesc(
                        principal.getChurchId(),
                        LiveStreamStatus.LIVE,
                        LocalDateTime.now().minus(MAX_LIVE_DURATION))
                .map(LiveStreamResponse::from)
                .orElse(null);
    }

    /** Past broadcasts — each still watchable on YouTube as a free recording. */
    @Transactional(readOnly = true)
    public Page<LiveStreamResponse> getStreams(MemberPrincipal principal, Pageable pageable) {
        return liveStreamRepository
                .findByChurchIdOrderByStartedAtDesc(principal.getChurchId(), pageable)
                .map(LiveStreamResponse::from);
    }

    public void deleteStream(UUID id, MemberPrincipal principal) {
        RoleChecker.requirePastorElderOrManager(principal);
        LiveStream stream = liveStreamRepository.findByChurchIdAndId(principal.getChurchId(), id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Live stream not found"));
        liveStreamRepository.delete(stream);
    }
}
