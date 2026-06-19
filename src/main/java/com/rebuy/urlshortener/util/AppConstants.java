package com.rebuy.urlshortener.util;

/**
 * Centralized constants — avoids magic numbers/strings scattered
 * across services, controllers, and configs.
 */
public final class AppConstants {

    private AppConstants() {
        // prevent instantiation
    }

    // ── Hash generation ──────────────────────────────────────
    public static final String BASE62_ALPHABET =
            "abcdefghijklmnopqrstuvwxyz" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "0123456789";

    public static final int HASH_LENGTH = 4;

    public static final int MAX_HASH_GENERATION_RETRIES = 10;

    // ── Validation ────────────────────────────────────────────
    public static final int MAX_URL_LENGTH = 2048;

    public static final String URL_SCHEME_REGEX = "^(https?://).*";

    public static final String HASH_PATH_REGEX = "^[a-zA-Z0-9]{4}$";

    // ── Caching ───────────────────────────────────────────────
    public static final String URL_CACHE_NAME = "urls";

    // ── HTTP headers ──────────────────────────────────────────
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
}