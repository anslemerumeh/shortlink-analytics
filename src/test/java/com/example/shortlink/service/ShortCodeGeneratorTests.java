package com.example.shortlink.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTests {

    private final ShortCodeGenerator generator = new ShortCodeGenerator();

    @Test
    void generatesCodeOfDefaultLength() {
        String code = generator.generate();
        assertThat(code).hasSize(7);
    }

    @Test
    void generatesCodeOfRequestedLength() {
        String code = generator.generate(12);
        assertThat(code).hasSize(12);
    }

    @Test
    void generatedCodeOnlyContainsAlphanumericCharacters() {
        String code = generator.generate(50);
        assertThat(code).matches("[A-Za-z0-9]+");
    }

    @Test
    void repeatedCallsProduceDifferentCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            codes.add(generator.generate());
        }
        // Astronomically unlikely to collide across 100 draws from a 62^7 space;
        // a collision here would indicate a broken random source, not bad luck.
        assertThat(codes).hasSize(100);
    }
}
