package com.rebuy.urlshortener.service;

import com.rebuy.urlshortener.dto.UrlDtos;
import com.rebuy.urlshortener.entity.ShortenedUrl;
import com.rebuy.urlshortener.exception.HashNotFoundException;
import com.rebuy.urlshortener.repository.ShortenedUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlShortenerService Tests")
class UrlShortenerServiceTest {

    @Mock
    private ShortenedUrlRepository repository;

    @Mock
    private HashGenerator hashGenerator;

    @InjectMocks
    private UrlShortenerService service;

    private static final String BASE_URL = "http://localhost:8080";
    private static final String ORIGINAL_URL = "https://amazon.com/product/123";
    private static final String HASH = "aB3x";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", BASE_URL);
    }

    @Test
    @DisplayName("shorten — creates new short URL for new URL")
    void shortenCreatesNewUrl() {
        when(repository.findByOriginalUrl(ORIGINAL_URL))
                .thenReturn(Optional.empty());
        when(hashGenerator.generate()).thenReturn(HASH);
        when(repository.existsByHash(HASH)).thenReturn(false);
        when(repository.save(any()))
                .thenReturn(buildEntity(HASH, ORIGINAL_URL));

        UrlDtos.ShortenResponse response = service.shorten(ORIGINAL_URL);

        assertThat(response.hash()).isEqualTo(HASH);
        assertThat(response.shortUrl())
                .isEqualTo(BASE_URL + "/" + HASH);
        assertThat(response.originalUrl()).isEqualTo(ORIGINAL_URL);
        verify(repository).save(any(ShortenedUrl.class));
    }

    @Test
    @DisplayName("shorten — returns existing hash for same URL (idempotent)")
    void shortenIsIdempotent() {
        ShortenedUrl existing = buildEntity(HASH, ORIGINAL_URL);
        when(repository.findByOriginalUrl(ORIGINAL_URL))
                .thenReturn(Optional.of(existing));

        UrlDtos.ShortenResponse response = service.shorten(ORIGINAL_URL);

        assertThat(response.hash()).isEqualTo(HASH);
        // Must NOT create a new entry
        verify(repository, never()).save(any());
        verify(hashGenerator, never()).generate();
    }

    @Test
    @DisplayName("shorten — retries on hash collision")
    void shortenRetriesOnCollision() {
        when(repository.findByOriginalUrl(ORIGINAL_URL))
                .thenReturn(Optional.empty());
        // First two collide, third is free
        when(hashGenerator.generate())
                .thenReturn("aaaa", "bbbb", HASH);
        when(repository.existsByHash("aaaa")).thenReturn(true);
        when(repository.existsByHash("bbbb")).thenReturn(true);
        when(repository.existsByHash(HASH)).thenReturn(false);
        when(repository.save(any()))
                .thenReturn(buildEntity(HASH, ORIGINAL_URL));

        UrlDtos.ShortenResponse response = service.shorten(ORIGINAL_URL);

        assertThat(response.hash()).isEqualTo(HASH);
        verify(hashGenerator, times(3)).generate();
    }

    @Test
    @DisplayName("resolve — returns original URL for valid hash")
    void resolveReturnsOriginalUrl() {
        when(repository.findByHash(HASH))
                .thenReturn(Optional.of(buildEntity(HASH, ORIGINAL_URL)));

        String result = service.resolve(HASH);

        assertThat(result).isEqualTo(ORIGINAL_URL);
    }

    @Test
    @DisplayName("resolve — throws HashNotFoundException for unknown hash")
    void resolveThrowsForUnknownHash() {
        when(repository.findByHash("xxxx"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("xxxx"))
                .isInstanceOf(HashNotFoundException.class)
                .hasMessageContaining("xxxx");
    }

    @Test
    @DisplayName("getStats — returns correct stats for valid hash")
    void getStatsReturnsStats() {
        ShortenedUrl entity = buildEntity(HASH, ORIGINAL_URL);
        entity.setAccessCount(42L);
        when(repository.findByHash(HASH))
                .thenReturn(Optional.of(entity));

        UrlDtos.StatsResponse stats = service.getStats(HASH);

        assertThat(stats.hash()).isEqualTo(HASH);
        assertThat(stats.accessCount()).isEqualTo(42L);
        assertThat(stats.originalUrl()).isEqualTo(ORIGINAL_URL);
        assertThat(stats.shortUrl())
                .isEqualTo(BASE_URL + "/" + HASH);
    }

    @Test
    @DisplayName("getStats — throws HashNotFoundException for unknown hash")
    void getStatsThrowsForUnknownHash() {
        when(repository.findByHash("xxxx"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStats("xxxx"))
                .isInstanceOf(HashNotFoundException.class);
    }

    // ── helpers ──────────────────────────────────────────────

    private ShortenedUrl buildEntity(String hash, String originalUrl) {
        return ShortenedUrl.builder()
                .id(1L)
                .hash(hash)
                .originalUrl(originalUrl)
                .createdAt(Instant.now())
                .accessCount(0L)
                .build();
    }
}