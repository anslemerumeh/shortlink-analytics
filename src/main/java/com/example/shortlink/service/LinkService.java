package com.example.shortlink.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shortlink.config.CacheConfig;
import com.example.shortlink.dto.LinkStatsResponse;
import com.example.shortlink.entity.ClickEvent;
import com.example.shortlink.entity.ShortLink;
import com.example.shortlink.exception.LinkNotFoundException;
import com.example.shortlink.repository.ClickEventRepository;
import com.example.shortlink.repository.ShortLinkRepository;

@Service
public class LinkService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 5;

    private final ShortLinkRepository shortLinkRepository;
    private final ClickEventRepository clickEventRepository;
    private final ShortCodeGenerator codeGenerator;

    public LinkService(
            ShortLinkRepository shortLinkRepository,
            ClickEventRepository clickEventRepository,
            ShortCodeGenerator codeGenerator) {
        this.shortLinkRepository = shortLinkRepository;
        this.clickEventRepository = clickEventRepository;
        this.codeGenerator = codeGenerator;
    }

    @Transactional
    public ShortLink createShortLink(String originalUrl, String customCode) {
        String code = (customCode != null && !customCode.isBlank())
                ? requireAvailable(customCode)
                : generateUniqueCode();

        ShortLink link = ShortLink.builder()
                .code(code)
                .originalUrl(originalUrl)
                .createdAt(Instant.now())
                .clickCount(0L)
                .build();

        return shortLinkRepository.save(link);
    }

    @Cacheable(cacheNames = CacheConfig.LINK_LOOKUP_CACHE, key = "#code")
    @Transactional(readOnly = true)
    public ShortLink getByCode(String code) {
        return shortLinkRepository.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
    }

    @CacheEvict(cacheNames = CacheConfig.LINK_LOOKUP_CACHE, key = "#code")
    @Transactional
    public void recordClick(String code, String referrer, String userAgent) {
        ShortLink link = shortLinkRepository.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));

        ClickEvent event = ClickEvent.builder()
                .shortLink(link)
                .clickedAt(Instant.now())
                .referrer(referrer)
                .userAgent(userAgent)
                .build();
        clickEventRepository.save(event);

        link.setClickCount(link.getClickCount() + 1);
        shortLinkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public LinkStatsResponse getStats(String code) {
        ShortLink link = shortLinkRepository.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
        Instant now = Instant.now();

        long total = clickEventRepository.countByShortLinkId(link.getId());
        long last24h = clickEventRepository.countByShortLinkIdAndClickedAtAfter(
                link.getId(), now.minus(24, ChronoUnit.HOURS));
        long last7d = clickEventRepository.countByShortLinkIdAndClickedAtAfter(
                link.getId(), now.minus(7, ChronoUnit.DAYS));

        return new LinkStatsResponse(link.getCode(), link.getOriginalUrl(), link.getCreatedAt(), total, last24h, last7d);
    }

    @Transactional(readOnly = true)
    public List<ShortLink> listLinks(Pageable pageable) {
        return shortLinkRepository.findAll(pageable).getContent();
    }

    private String requireAvailable(String customCode) {
        if (shortLinkRepository.existsByCode(customCode)) {
            throw new IllegalStateException("Short code '" + customCode + "' is already taken");
        }
        return customCode;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String candidate = codeGenerator.generate();
            if (!shortLinkRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique short code, please retry");
    }
}
