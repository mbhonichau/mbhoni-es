package com.mbhoni_creative.api.dto;

import java.time.LocalDate;
import java.util.List;

public record BillingDraftRequest(String customerName, String customerEmail, String customerPhone,
        String customerAddress, LocalDate issueDate, LocalDate dueDate, LocalDate validUntilDate,
        String notes, List<Line> items) {
    public record Line(String description, String quantity, String unitPrice) {}
}
