package com.example.demo;

import com.example.demo.model.LiveStreamProvider;
import com.example.demo.service.LiveStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The YouTube link a leader pastes is the one piece of free-text input in the
 * live-stream flow, so every shape YouTube hands out must resolve to the same
 * 11-char video id — and anything else must be rejected before it reaches the DB.
 */
class LiveStreamServiceTest {

    private static final String ID = "dQw4w9WgXcQ";

    @Test
    void parsesStandardWatchUrl() {
        assertEquals(ID, LiveStreamService.extractYoutubeVideoId("https://www.youtube.com/watch?v=" + ID));
    }

    @Test
    void parsesShortYoutuBeUrl() {
        assertEquals(ID, LiveStreamService.extractYoutubeVideoId("https://youtu.be/" + ID));
    }

    @Test
    void parsesLiveUrl() {
        // The form YouTube gives for an actual live broadcast.
        assertEquals(ID, LiveStreamService.extractYoutubeVideoId("https://www.youtube.com/live/" + ID));
    }

    @Test
    void parsesEmbedAndShortsUrls() {
        assertEquals(ID, LiveStreamService.extractYoutubeVideoId("https://www.youtube.com/embed/" + ID));
        assertEquals(ID, LiveStreamService.extractYoutubeVideoId("https://www.youtube.com/shorts/" + ID));
    }

    @Test
    void parsesUrlWithExtraQueryParams() {
        // Share links routinely carry ?si=, &t=, &feature= — none may confuse it.
        assertEquals(ID, LiveStreamService.extractYoutubeVideoId(
                "https://www.youtube.com/watch?v=" + ID + "&t=30s&feature=share"));
        assertEquals(ID, LiveStreamService.extractYoutubeVideoId(
                "https://youtu.be/" + ID + "?si=AbCdEfGhIjKlMnOp"));
    }

    @Test
    void parsesBareVideoId() {
        assertEquals(ID, LiveStreamService.extractYoutubeVideoId(ID));
    }

    @Test
    void trimsSurroundingWhitespaceFromPastedLink() {
        assertEquals(ID, LiveStreamService.extractYoutubeVideoId("  https://youtu.be/" + ID + "  "));
    }

    @Test
    void rejectsNonYoutubeLink() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> LiveStreamService.extractYoutubeVideoId("https://example.com/not-a-video"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // ── Provider detection ────────────────────────────────────────────────

    @Test
    void detectsYoutubeAndExtractsId() {
        LiveStreamService.ParsedSource parsed =
                LiveStreamService.parseSource("https://www.youtube.com/watch?v=" + ID);
        assertEquals(LiveStreamProvider.YOUTUBE, parsed.provider());
        assertEquals(ID, parsed.sourceRef());
    }

    @Test
    void detectsFacebookAndKeepsWholeUrl() {
        // Facebook's embed needs the full post URL as an href, not an id.
        String url = "https://www.facebook.com/mychurch/videos/1234567890/";
        LiveStreamService.ParsedSource parsed = LiveStreamService.parseSource(url);
        assertEquals(LiveStreamProvider.FACEBOOK, parsed.provider());
        assertEquals(url, parsed.sourceRef());
    }

    @Test
    void detectsFacebookAcrossItsManyHostForms() {
        for (String url : new String[]{
                "https://facebook.com/watch/?v=1234567890",
                "https://web.facebook.com/mychurch/videos/1234567890/",
                "https://m.facebook.com/watch/live/?v=1234567890",
                "https://fb.watch/aBcDeFgHiJ/",
        }) {
            assertEquals(LiveStreamProvider.FACEBOOK, LiveStreamService.parseSource(url).provider(),
                    "should detect Facebook for " + url);
        }
    }

    @Test
    void parseSourceTrimsAndRejectsRubbish() {
        assertEquals(LiveStreamProvider.YOUTUBE,
                LiveStreamService.parseSource("  https://youtu.be/" + ID + " ").provider());
        assertThrows(ResponseStatusException.class,
                () -> LiveStreamService.parseSource("https://example.com/whatever"));
        assertThrows(ResponseStatusException.class, () -> LiveStreamService.parseSource("   "));
    }

    @Test
    void rejectsBlankAndNull() {
        assertThrows(ResponseStatusException.class, () -> LiveStreamService.extractYoutubeVideoId("   "));
        assertThrows(ResponseStatusException.class, () -> LiveStreamService.extractYoutubeVideoId(null));
    }

    @Test
    void rejectsIdOfWrongLength() {
        // 10 chars — must not be accepted as a bare id.
        assertThrows(ResponseStatusException.class, () -> LiveStreamService.extractYoutubeVideoId("shortId123"));
    }
}
