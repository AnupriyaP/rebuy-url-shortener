package com.rebuy.urlshortener.service;

import com.rebuy.urlshortener.dto.UrlDtos;
import com.rebuy.urlshortener.entity.ShortenedUrl;
import com.rebuy.urlshortener.exception.HashNotFoundException;
import com.rebuy.urlshortener.exception.UrlShortenerException;
import com.rebuy.urlshortener.repository.ShortenedUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rebuy.urlshortener.util.AppConstants;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    .withZone(ZoneOffset.UTC);

    private final ShortenedUrlRepository repository;
    private final HashGenerator hashGenerator;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Shorten a URL.
     *
     * Idempotent — same URL always returns same hash.
     * Without idempotency, same URL gets hundreds of different hashes.
     */
    @Transactional
    public UrlDtos.ShortenResponse shorten(String originalUrl) {

        // Already shortened? Return existing hash
        return repository.findByOriginalUrl(originalUrl)
                .map(existing -> {
                    log.debug("URL already exists: {} -> {}",
                            originalUrl, existing.getHash());
                    return toResponse(existing);
                })
                .orElseGet(() -> createNew(originalUrl));
    }

    private UrlDtos.ShortenResponse createNew(String originalUrl) {
        String hash = generateUniqueHash();

        ShortenedUrl entity = ShortenedUrl.builder()
                .hash(hash)
                .originalUrl(originalUrl)
                .accessCount(0L)
                .build();

        ShortenedUrl saved = repository.save(entity);
        log.info("Created short URL: {} -> {}", hash, originalUrl);
        return toResponse(saved);
    }

    private String generateUniqueHash() {
        for (int attempt = 0; attempt < AppConstants.MAX_HASH_GENERATION_RETRIES; attempt++) {
            String hash = hashGenerator.generate();
            if (!repository.existsByHash(hash)) {
                return hash;
            }
            log.warn("Hash collision on attempt {}: {}", attempt + 1, hash);
        }
        log.error("Exhausted {} hash generation attempts — possible hash space exhaustion", AppConstants.MAX_HASH_GENERATION_RETRIES);
        throw new UrlShortenerException(
                "Could not generate unique hash after "
                        + AppConstants.MAX_HASH_GENERATION_RETRIES + " attempts. Please try again."
        );
    }

    /**
     * Resolve hash to original URL.
     *
     * Cached in Redis — this is the hottest endpoint.
     * A single campaign email to 100k customers = 100k hits
     * on same hash. Cache means DB hit only once ever per hash.
     */
    @Cacheable(value = AppConstants.URL_CACHE_NAME, key = "#hash")
    @Transactional(readOnly = true)
    public String resolve(String hash) {
        return repository.findByHash(hash)
                .map(ShortenedUrl::getOriginalUrl)
                .orElseThrow(() -> new HashNotFoundException(hash));
    }

    /**
     * Record a click — increments access count in DB.
     * Uses direct UPDATE query — no need to load entity.
     *
     * Passes Instant.now() explicitly rather than relying on the
     * database's CURRENT_TIMESTAMP, since newer Hibernate versions
     * strictly validate that the JPQL expression type matches the
     * entity field type (Instant). Generating the timestamp in Java
     * guarantees the type always matches, and is also more testable.
     */
    @Transactional
    public void recordAccess(String hash) {
        repository.incrementAccessCount(hash, Instant.now());
        log.debug("Access count incremented for hash: {}", hash);
    }

    /**
     * Get stats for a hash — used by marketing team
     * to measure campaign performance.
     */
    @Transactional(readOnly = true)
    public UrlDtos.StatsResponse getStats(String hash) {
        ShortenedUrl url = repository.findByHash(hash)
                .orElseThrow(() -> new HashNotFoundException(hash));

        return new UrlDtos.StatsResponse(
                url.getHash(),
                url.getOriginalUrl(),
                buildShortUrl(url.getHash()),
                url.getAccessCount(),
                FORMATTER.format(url.getCreatedAt()),
                url.getLastAccessedAt() != null
                        ? FORMATTER.format(url.getLastAccessedAt())
                        : null
        );
    }

    private UrlDtos.ShortenResponse toResponse(ShortenedUrl entity) {
        return new UrlDtos.ShortenResponse(
                entity.getOriginalUrl(),
                buildShortUrl(entity.getHash()),
                entity.getHash(),
                FORMATTER.format(entity.getCreatedAt())
        );
    }

    private String buildShortUrl(String hash) {
        return baseUrl + "/" + hash;
    }
}