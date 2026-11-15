package com.mbhoni_creative.api.service;

import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ApiIdempotencyCleanup {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    public ApiIdempotencyCleanup(JdbcTemplate jdbc,Clock clock) { this.jdbc=jdbc;this.clock=clock; }
    @Scheduled(fixedDelayString="${api.billing.idempotency.cleanup-ms:3600000}")
    public void expire() { jdbc.update("DELETE FROM api_idempotency WHERE expires_at<=?",LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC)); }
}
