package com.techora.domain.category.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugServiceTest {
    private final SlugService slugService = new SlugService();

    @Test
    void createSlugNormalizesReadableText() {
        assertThat(slugService.createSlug("Áo Thun Nam 2026!"))
                .isEqualTo("ao-thun-nam-2026");
    }

    @Test
    void createSlugReturnsFallbackForBlankResult() {
        assertThat(slugService.createSlug("!!!"))
                .isEqualTo("item");
    }
}
