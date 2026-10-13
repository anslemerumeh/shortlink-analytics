package com.example.shortlink.repository;

import com.example.shortlink.entity.ClickEvent;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortLinkId(Long shortLinkId);

    long countByShortLinkIdAndClickedAtAfter(Long shortLinkId, Instant after);
}
