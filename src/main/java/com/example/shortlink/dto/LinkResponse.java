package com.example.shortlink.dto;

import java.time.Instant;

import com.example.shortlink.entity.ShortLink;

public record LinkResponse(String code, String shortUrl, String originalUrl, Instant createdAt, long clickCount) {

    public static LinkResponse from(ShortLink link, String baseUrl) {
        return new LinkResponse(
                link.getCode(),
                baseUrl + "/" + link.getCode(),
                link.getOriginalUrl(),
                link.getCreatedAt(),
                link.getClickCount());
    }
}
