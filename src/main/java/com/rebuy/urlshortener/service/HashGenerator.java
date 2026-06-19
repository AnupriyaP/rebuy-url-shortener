package com.rebuy.urlshortener.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.rebuy.urlshortener.util.AppConstants;
import java.security.SecureRandom;

/**
 * Generates 4-character Base62 hashes.
 *
 * Alphabet : a-z, A-Z, 0-9  (62 characters)
 * Capacity : 62^4 = 14,776,336 unique combinations
 *
 * Why random over counter-based:
 *   - Unpredictable — nobody can enumerate other people's links
 *   - Simple — no distributed coordination needed at MVP scale
 *   - Collision handled by retry loop in UrlShortenerService
 *
 * Why SecureRandom over Random:
 *   - Cryptographically strong — truly unpredictable
 *   - Thread safe
 */
@Component
@Slf4j
public class HashGenerator {

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(AppConstants.HASH_LENGTH);
        for (int i = 0; i < AppConstants.HASH_LENGTH; i++) {
            sb.append(AppConstants.BASE62_ALPHABET.charAt(
                    random.nextInt(AppConstants.BASE62_ALPHABET.length())
            ));
        }
        String hash = sb.toString();
        // trace level deliberately — this runs on every shorten request,
        // sometimes multiple times on retry. info/debug here would flood logs.
        log.trace("Generated hash candidate: {}", hash);
        return hash;
    }
}