package com.example.shortlink.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.shortlink.dto.LinkStatsResponse;
import com.example.shortlink.entity.ShortLink;
import com.example.shortlink.exception.LinkNotFoundException;
import com.example.shortlink.service.LinkService;

@WebMvcTest(LinkController.class)
class LinkControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LinkService linkService;

    private ShortLink sampleLink() {
        return ShortLink.builder()
                .id(1L)
                .code("abc1234")
                .originalUrl("https://example.com/page")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .clickCount(5L)
                .build();
    }

    @Test
    void createLinkReturns201WithShortUrl() throws Exception {
        when(linkService.createShortLink(eq("https://example.com/page"), eq(null))).thenReturn(sampleLink());

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/page\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("abc1234"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc1234"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/page"));
    }

    @Test
    void createLinkRejectsInvalidUrl() throws Exception {
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createLinkRejectsBlankUrl() throws Exception {
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLinkReturns404WhenMissing() throws Exception {
        when(linkService.getByCode("missing")).thenThrow(new LinkNotFoundException("missing"));

        mockMvc.perform(get("/api/links/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getLinkReturnsLinkDetails() throws Exception {
        when(linkService.getByCode("abc1234")).thenReturn(sampleLink());

        mockMvc.perform(get("/api/links/abc1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("abc1234"))
                .andExpect(jsonPath("$.clickCount").value(5));
    }

    @Test
    void getStatsReturnsAggregatedCounts() throws Exception {
        when(linkService.getStats("abc1234"))
                .thenReturn(new LinkStatsResponse(
                        "abc1234", "https://example.com/page", Instant.parse("2026-01-01T00:00:00Z"), 10L, 2L, 7L));

        mockMvc.perform(get("/api/links/abc1234/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(10))
                .andExpect(jsonPath("$.clicksLast24Hours").value(2))
                .andExpect(jsonPath("$.clicksLast7Days").value(7));
    }

    @Test
    void listLinksReturnsAllLinks() throws Exception {
        when(linkService.listLinks(any(PageRequest.class))).thenReturn(List.of(sampleLink()));

        mockMvc.perform(get("/api/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("abc1234"));
    }
}
