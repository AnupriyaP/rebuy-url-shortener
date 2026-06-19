package com.rebuy.urlshortener.dto;

import com.rebuy.urlshortener.util.AppConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UrlDtos {

    // ── What the user sends us ──────────────────────
    public record ShortenRequest(

            @NotBlank(message = "URL must not be blank")
            @Pattern(
                    regexp = AppConstants.URL_SCHEME_REGEX,
                    message = "URL must start with http:// or https://"
            )
            @Size(max = AppConstants.MAX_URL_LENGTH, message = "URL must not exceed 2048 characters")
            String url

    ) {}

    // ── What we send back after shortening ──────────
    public record ShortenResponse(
            String originalUrl,
            String shortUrl,
            String hash,
            String createdAt
    ) {}

    // ── What we send back for stats ─────────────────
    public record StatsResponse(
            String hash,
            String originalUrl,
            String shortUrl,
            long accessCount,
            String createdAt,
            String lastAccessedAt
    ) {}

    // ── What we send back on errors ─────────────────
    public record ErrorResponse(
            String error,
            String message,
            int status
    ) {}


}