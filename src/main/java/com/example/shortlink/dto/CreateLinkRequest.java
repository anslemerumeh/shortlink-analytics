package com.example.shortlink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLinkRequest(
        @NotBlank(message = "url is required")
        @Pattern(regexp = "^https?://.+", message = "url must start with http:// or https://")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        String url,

        @Pattern(regexp = "^[A-Za-z0-9_-]{3,16}$", message = "customCode must be 3-16 alphanumeric characters, - or _")
        String customCode) {
}
