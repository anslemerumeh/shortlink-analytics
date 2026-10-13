package com.example.shortlink.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shortlink.dto.CreateLinkRequest;
import com.example.shortlink.dto.LinkResponse;
import com.example.shortlink.dto.LinkStatsResponse;
import com.example.shortlink.entity.ShortLink;
import com.example.shortlink.service.LinkService;

@RestController
@RequestMapping("/api/links")
public class LinkController {

    private final LinkService linkService;
    private final String baseUrl;

    public LinkController(LinkService linkService, @Value("${app.base-url}") String baseUrl) {
        this.linkService = linkService;
        this.baseUrl = baseUrl;
    }

    @PostMapping
    public ResponseEntity<LinkResponse> createLink(@Valid @RequestBody CreateLinkRequest request) {
        ShortLink link = linkService.createShortLink(request.url(), request.customCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(LinkResponse.from(link, baseUrl));
    }

    @GetMapping
    public List<LinkResponse> listLinks(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return linkService.listLinks(pageable).stream().map(link -> LinkResponse.from(link, baseUrl)).toList();
    }

    @GetMapping("/{code}")
    public LinkResponse getLink(@PathVariable String code) {
        return LinkResponse.from(linkService.getByCode(code), baseUrl);
    }

    @GetMapping("/{code}/stats")
    public LinkStatsResponse getStats(@PathVariable String code) {
        return linkService.getStats(code);
    }
}
