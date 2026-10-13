package com.example.shortlink.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.shortlink.dto.LinkStatsResponse;
import com.example.shortlink.entity.ClickEvent;
import com.example.shortlink.entity.ShortLink;
import com.example.shortlink.exception.LinkNotFoundException;
import com.example.shortlink.repository.ClickEventRepository;
import com.example.shortlink.repository.ShortLinkRepository;

@ExtendWith(MockitoExtension.class)
class LinkServiceTests {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private ShortCodeGenerator codeGenerator;

    private LinkService linkService;

    @BeforeEach
    void setUp() {
        linkService = new LinkService(shortLinkRepository, clickEventRepository, codeGenerator);
    }

    @Test
    void createShortLinkGeneratesCodeWhenNoneProvided() {
        when(codeGenerator.generate()).thenReturn("abc1234");
        when(shortLinkRepository.existsByCode("abc1234")).thenReturn(false);
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortLink result = linkService.createShortLink("https://example.com", null);

        assertThat(result.getCode()).isEqualTo("abc1234");
        assertThat(result.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(result.getClickCount()).isZero();
    }

    @Test
    void createShortLinkRetriesOnCollision() {
        when(codeGenerator.generate()).thenReturn("taken11", "fresh22");
        when(shortLinkRepository.existsByCode("taken11")).thenReturn(true);
        when(shortLinkRepository.existsByCode("fresh22")).thenReturn(false);
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortLink result = linkService.createShortLink("https://example.com", null);

        assertThat(result.getCode()).isEqualTo("fresh22");
    }

    @Test
    void createShortLinkUsesCustomCodeWhenAvailable() {
        when(shortLinkRepository.existsByCode("my-code")).thenReturn(false);
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortLink result = linkService.createShortLink("https://example.com", "my-code");

        assertThat(result.getCode()).isEqualTo("my-code");
    }

    @Test
    void createShortLinkRejectsTakenCustomCode() {
        when(shortLinkRepository.existsByCode("taken")).thenReturn(true);

        assertThatThrownBy(() -> linkService.createShortLink("https://example.com", "taken"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("taken");
    }

    @Test
    void getByCodeThrowsWhenMissing() {
        when(shortLinkRepository.findByCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> linkService.getByCode("missing")).isInstanceOf(LinkNotFoundException.class);
    }

    @Test
    void recordClickSavesEventAndIncrementsCount() {
        ShortLink link = ShortLink.builder()
                .id(1L)
                .code("abc1234")
                .originalUrl("https://example.com")
                .createdAt(Instant.now())
                .clickCount(4L)
                .build();
        when(shortLinkRepository.findByCode("abc1234")).thenReturn(Optional.of(link));
        when(shortLinkRepository.save(any(ShortLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        linkService.recordClick("abc1234", "https://referrer.example", "test-agent");

        ArgumentCaptor<ClickEvent> eventCaptor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getReferrer()).isEqualTo("https://referrer.example");
        assertThat(eventCaptor.getValue().getUserAgent()).isEqualTo("test-agent");

        ArgumentCaptor<ShortLink> linkCaptor = ArgumentCaptor.forClass(ShortLink.class);
        verify(shortLinkRepository).save(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getClickCount()).isEqualTo(5L);
    }

    @Test
    void getStatsAggregatesClickCounts() {
        ShortLink link = ShortLink.builder()
                .id(1L)
                .code("abc1234")
                .originalUrl("https://example.com")
                .createdAt(Instant.now())
                .clickCount(10L)
                .build();
        when(shortLinkRepository.findByCode("abc1234")).thenReturn(Optional.of(link));
        when(clickEventRepository.countByShortLinkId(1L)).thenReturn(10L);
        when(clickEventRepository.countByShortLinkIdAndClickedAtAfter(eq(1L), any(Instant.class)))
                .thenReturn(3L);

        LinkStatsResponse stats = linkService.getStats("abc1234");

        assertThat(stats.code()).isEqualTo("abc1234");
        assertThat(stats.totalClicks()).isEqualTo(10L);
        assertThat(stats.clicksLast24Hours()).isEqualTo(3L);
        assertThat(stats.clicksLast7Days()).isEqualTo(3L);
    }

    @Test
    void listLinksReturnsPageContent() {
        ShortLink link = ShortLink.builder().id(1L).code("abc1234").originalUrl("https://example.com")
                .createdAt(Instant.now()).clickCount(0L).build();
        when(shortLinkRepository.findAll(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(link)));

        List<ShortLink> result = linkService.listLinks(PageRequest.of(0, 20));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("abc1234");
    }
}
