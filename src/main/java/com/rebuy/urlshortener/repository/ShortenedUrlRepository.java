package com.rebuy.urlshortener.repository;

import com.rebuy.urlshortener.entity.ShortenedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface ShortenedUrlRepository
        extends JpaRepository<ShortenedUrl, Long> {

    Optional<ShortenedUrl> findByHash(String hash);

    Optional<ShortenedUrl> findByOriginalUrl(String originalUrl);

    boolean existsByHash(String hash);

    @Modifying
    @Query("""
        UPDATE ShortenedUrl s
        SET s.accessCount = s.accessCount + 1,
            s.lastAccessedAt = :now
        WHERE s.hash = :hash
    """)
    void incrementAccessCount(@Param("hash") String hash, @Param("now") Instant now);
}