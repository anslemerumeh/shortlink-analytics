package com.example.shortlink.dto;

import java.time.Instant;

public record LinkStatsResponse(
        String code,
        String originalUrl,
        Instant createdAt,
        long totalClicks,
        long clicksLast24Hours,
        long clicksLast7Days) {
}
