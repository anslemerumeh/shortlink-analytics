package com.example.shortlink.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.shortlink.entity.ShortLink;
import com.example.shortlink.exception.LinkNotFoundException;
import com.example.shortlink.service.LinkService;

@WebMvcTest(RedirectController.class)
class RedirectControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LinkService linkService;

    @Test
    void redirectsToOriginalUrlAndRecordsClick() throws Exception {
        ShortLink link = ShortLink.builder()
                .id(1L)
                .code("abc1234")
                .originalUrl("https://example.com/page")
                .createdAt(Instant.now())
                .clickCount(0L)
                .build();
        when(linkService.getByCode("abc1234")).thenReturn(link);

        mockMvc.perform(get("/abc1234").header("User-Agent", "test-agent"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/page"));

        verify(linkService).recordClick(eq("abc1234"), any(), eq("test-agent"));
    }

    @Test
    void returns404ForUnknownCode() throws Exception {
        when(linkService.getByCode("missing")).thenThrow(new LinkNotFoundException("missing"));

        mockMvc.perform(get("/missing")).andExpect(status().isNotFound());
    }
}
