package com.rebuy.urlshortener.controller;

import com.rebuy.urlshortener.dto.UrlDtos;
import com.rebuy.urlshortener.service.RateLimiterService;
import com.rebuy.urlshortener.service.UrlShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.rebuy.urlshortener.util.AppConstants;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "URL Shortener", description = "Shorten URLs and manage redirects")
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;
    private final RateLimiterService rateLimiterService;

    /**
     * POST /api/shorten
     * Accepts a long URL and returns a shortened version.
     * Rate limited per IP — max 10 requests per minute.
     */
    @Operation(
            summary = "Shorten a URL",
            description = "Takes a long URL and returns a 4-character short URL"
    )
    @PostMapping("/api/v1/shorten")
    public ResponseEntity<UrlDtos.ShortenResponse> shorten(
            @Valid @RequestBody UrlDtos.ShortenRequest request,
            HttpServletRequest httpRequest) {

        // Rate limit check — per IP
        String clientIp = getClientIp(httpRequest);
        rateLimiterService.checkLimit(clientIp);

        log.info("Shorten request from IP: {} for URL: {}",
                clientIp, request.url());

        UrlDtos.ShortenResponse response =
                urlShortenerService.shorten(request.url());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * GET /{hash}
     * Redirects to the original URL.
     *
     * Uses 302 (temporary) NOT 301 (permanent) because:
     * - 301 gets cached by browsers forever
     * - We lose ability to update/delete/track links
     * - Analytics require every click to hit our server
     */
    @Operation(
            summary = "Redirect to original URL",
            description = "Returns 302 Found with a Location header pointing to the original " +
                    "URL. Real browsers follow this automatically. NOTE: this endpoint cannot " +
                    "be meaningfully tested via Swagger's 'Try it out' — browsers block the " +
                    "cross-origin fetch to the destination site after following the redirect. " +
                    "Use the generated curl command with -i (shown below) or paste the URL " +
                    "directly into your browser's address bar instead."
    )
    @GetMapping("/{hash:" + AppConstants.HASH_PATH_REGEX + "}")
    public ResponseEntity<Void> redirect(
            @PathVariable String hash) {

        log.debug("Redirect request for hash: {}", hash);

        String originalUrl = urlShortenerService.resolve(hash);

        urlShortenerService.recordAccess(hash);

        log.info("Redirected hash {} to {}", hash, originalUrl);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                .build();
    }

    /**
     * GET /api/v1/stats/{hash}
     * Returns click analytics for a short URL.
     * Used by marketing team to measure campaign performance.
     */
    @Operation(
            summary = "Get URL statistics",
            description = "Returns click count and other stats for a short URL"
    )
    @GetMapping("/api/v1/stats/{hash}")
    public ResponseEntity<UrlDtos.StatsResponse> stats(
            @PathVariable String hash) {

        log.debug("Stats request for hash: {}", hash);

        return ResponseEntity.ok(
                urlShortenerService.getStats(hash)
        );
    }

    /**
     * Extract real client IP.
     *
     * NOTE: X-Forwarded-For is trusted here for reverse-proxy compatibility,
     * but this header is client-controlled and spoofable. In production this
     * is only safe behind a proxy/load balancer that overwrites this header
     * before it reaches the app (e.g. nginx, AWS ALB). Direct client requests
     * could spoof this and bypass rate limiting.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader(AppConstants.HEADER_X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isEmpty()) {
            String ip = forwarded.split(",")[0].trim();
            log.trace("Resolved client IP from X-Forwarded-For: {}", ip);
            return ip;
        }
        String ip = request.getRemoteAddr();
        log.trace("Resolved client IP from remote address: {}", ip);
        return ip;
    }
}