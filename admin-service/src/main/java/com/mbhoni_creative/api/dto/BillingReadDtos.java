package com.mbhoni_creative.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Stored amounts are decimal strings; no financial calculations occur on reads. */
public final class BillingReadDtos {
    private BillingReadDtos() {}
    public record Line(Long id, String description, String quantity, String unitPrice, String taxRate, String totalAmount) {}
    public record Invoice(Long id, String number, String customer, String status, LocalDate issueDate, LocalDate dueDate,
            String subtotal, String taxAmount, String totalAmount, String amountPaid, LocalDateTime updatedAt, List<Line> items) {}
    public record Quotation(Long id, String number, String customer, String status, LocalDate issueDate, LocalDate validUntilDate,
            String subtotal, String taxAmount, String totalAmount, LocalDateTime updatedAt, List<Line> items) {}
    public record Expense(Long id, LocalDate expenseDate, String vendor, String category, String reference,
            String amount, String taxAmount, LocalDateTime updatedAt) {}
}
