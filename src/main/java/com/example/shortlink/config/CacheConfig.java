package com.example.shortlink.config;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Caches short-code -> link lookups in memory with Caffeine, since redirects
 * are the hottest path in a URL shortener and don't need per-request DB hits.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String LINK_LOOKUP_CACHE = "linkLookup";

    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> caffeineCacheManagerCustomizer() {
        return cacheManager -> cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(10, TimeUnit.MINUTES));
    }
}
