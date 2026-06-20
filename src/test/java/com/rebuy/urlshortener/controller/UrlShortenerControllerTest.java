package com.rebuy.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rebuy.urlshortener.dto.UrlDtos;
import com.rebuy.urlshortener.entity.ShortenedUrl;
import com.rebuy.urlshortener.repository.ShortenedUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasLength;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UrlShortenerController Integration Tests")
class UrlShortenerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortenedUrlRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // ── POST /api/v1/shorten ─────────────────────────────────────

    @Test
    @DisplayName("POST /api/v1/shorten — 201 for valid URL")
    void shortenValidUrl() throws Exception {
        var request = new UrlDtos.ShortenRequest(
                "https://www.amazon.com/product/123"
        );

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hash").isString())
                .andExpect(jsonPath("$.hash", hasLength(4)))
                .andExpect(jsonPath("$.shortUrl").isString())
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://www.amazon.com/product/123"))
                .andExpect(jsonPath("$.createdAt").isString());
    }

    @Test
    @DisplayName("POST /api/v1/shorten — same URL returns same hash (idempotent)")
    void shortenSameUrlTwiceReturnsIdenticalHash() throws Exception {
        var request = new UrlDtos.ShortenRequest(
                "https://www.rebuy.com/product/456"
        );
        String body = objectMapper.writeValueAsString(request);

        String response1 = mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String response2 = mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var r1 = objectMapper.readValue(
                response1, UrlDtos.ShortenResponse.class);
        var r2 = objectMapper.readValue(
                response2, UrlDtos.ShortenResponse.class);

        assertThat(r1.hash()).isEqualTo(r2.hash());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/v1/shorten — 400 for blank URL")
    void shortenBlankUrl() throws Exception {
        var request = new UrlDtos.ShortenRequest("");

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/shorten — 400 for URL without https")
    void shortenInvalidScheme() throws Exception {
        var request = new UrlDtos.ShortenRequest("ftp://some-server.com");

        mockMvc.perform(post("/api/v1/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    // ── GET /{hash} redirect ──────────────────────────────────

    @Test
    @DisplayName("GET /{hash} — 302 redirect to original URL")
    void redirectValidHash() throws Exception {
        repository.save(buildEntity(
                "aB3x", "https://www.amazon.com/product/123"
        ));

        mockMvc.perform(get("/aB3x"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location", "https://www.amazon.com/product/123"
                ));
    }

    @Test
    @DisplayName("GET /{hash} — 404 for unknown hash")
    void redirectUnknownHash() throws Exception {
        mockMvc.perform(get("/xxxx"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /{hash} — increments access count")
    void redirectIncrementsAccessCount() throws Exception {
        repository.save(buildEntity(
                "cR4t", "https://www.rebuy.com"
        ));

        mockMvc.perform(get("/cR4t"))
                .andExpect(status().isFound());

        ShortenedUrl updated = repository
                .findByHash("cR4t").orElseThrow();
        assertThat(updated.getAccessCount()).isEqualTo(1L);
    }

    // ── GET /api/stats/{hash} ─────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/stats/{hash} — returns stats")
    void statsValidHash() throws Exception {
        repository.save(buildEntity(
                "Zq1w", "https://shop.com/product/789"
        ));

        mockMvc.perform(get("/api/v1/stats/Zq1w"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hash").value("Zq1w"))
                .andExpect(jsonPath("$.originalUrl")
                        .value("https://shop.com/product/789"))
                .andExpect(jsonPath("$.accessCount").value(0))
                .andExpect(jsonPath("$.shortUrl").isString())
                .andExpect(jsonPath("$.createdAt").isString());
    }

    @Test
    @DisplayName("GET /api/v1/stats/{hash} — 404 for unknown hash")
    void statsUnknownHash() throws Exception {
        mockMvc.perform(get("/api/v1/stats/zzzz"))
                .andExpect(status().isNotFound());
    }

    // ── helpers ───────────────────────────────────────────────

    private ShortenedUrl buildEntity(String hash, String originalUrl) {
        return ShortenedUrl.builder()
                .hash(hash)
                .originalUrl(originalUrl)
                .createdAt(Instant.now())
                .accessCount(0L)
                .build();
    }
}