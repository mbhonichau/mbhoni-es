package com.mbhoni_creative.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BillingWriteSettings implements ApplicationRunner {
    private final boolean enabled;
    public BillingWriteSettings(@Value("${api.billing.writes.enabled:false}") boolean enabled) { this.enabled=enabled; }
    public boolean enabled() { return enabled; }
    @Override public void run(ApplicationArguments args) {
        if(enabled) org.slf4j.LoggerFactory.getLogger(getClass()).warn(
                "API billing writes enabled: API drafts are counted in UI aggregates, appear in the owner export, and advance UI document numbering. Do not enable in production until the aggregate exclusion task is complete.");
    }
}
