package com.example.demo.model;

/**
 * Where a church's broadcast actually lives. Klink never hosts the video — it
 * only records which platform is carrying it so the app can embed the right
 * player.
 */
public enum LiveStreamProvider {
    /** sourceRef holds the 11-char YouTube video id. */
    YOUTUBE,
    /** sourceRef holds the full Facebook video/post URL (the embed needs it). */
    FACEBOOK
}
