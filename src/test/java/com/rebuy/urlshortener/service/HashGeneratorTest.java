package com.rebuy.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HashGenerator Tests")
class HashGeneratorTest {

    private HashGenerator hashGenerator;

    @BeforeEach
    void setUp() {
        hashGenerator = new HashGenerator();
    }

    @Test
    @DisplayName("Generated hash is exactly 4 characters")
    void hashIsExactlyFourChars() {
        String hash = hashGenerator.generate();
        assertThat(hash).hasSize(4);
    }

    @Test
    @DisplayName("Generated hash contains only Base62 characters")
    void hashContainsOnlyBase62Chars() {
        for (int i = 0; i < 1000; i++) {
            String hash = hashGenerator.generate();
            assertThat(hash).matches("[a-zA-Z0-9]{4}");
        }
    }

    @Test
    @DisplayName("Hash contains no special URL characters")
    void hashContainsNoSpecialUrlChars() {
        for (int i = 0; i < 1000; i++) {
            String hash = hashGenerator.generate();
            assertThat(hash)
                    .doesNotContain("?", "#", "&", "/", "%", "+", "=", " ");
        }
    }

    @RepeatedTest(5)
    @DisplayName("Generates highly unique hashes across many calls")
    void generatesUniqueHashes() {
        Set<String> hashes = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            hashes.add(hashGenerator.generate());
        }
        // 62^4 = 14.7M combinations
        // 10k generates should have very high uniqueness
        assertThat(hashes.size()).isGreaterThan(9_900);
    }
}