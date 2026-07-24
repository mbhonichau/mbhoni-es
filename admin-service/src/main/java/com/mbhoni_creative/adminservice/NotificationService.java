package com.mbhoni_creative.adminservice;

public interface NotificationService {

    void sendNewUserAlert(String username, String email, String tenantName);

    void sendRegistrationEmail(String username, String email, String resetLink, String tenantName);

    void sendWelcomeEmail(String username, String email, String tenantName);

    void sendPasswordResetAlert(String username, String email);

    void sendInvoiceOverdueAlert(String tenantName, String invoiceNumber, double amount, String dueDate);

    void sendTenantSuspensionAlert(String tenantName, String reason);

    void sendSubscriptionChangeAlert(String tenantName, String oldPlan, String newPlan);
}
