package com.mbhoni_creative.billing;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stripe_webhook_receipts")
public class WebhookReceipt {
    @Id private String eventId;
    private Instant processedAt;
    private String outcome;
    protected WebhookReceipt() {}
    public WebhookReceipt(String eventId, Instant processedAt, String outcome) {
        this.eventId = eventId; this.processedAt = processedAt; this.outcome = outcome;
    }
}
