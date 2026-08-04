package com.mbhoni_creative.adminservice.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.mbhoni_creative.adminservice.NotificationService;

import jakarta.mail.internet.MimeMessage;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@mbhoni-es.com}")
    private String fromEmail;

    @Value("${app.admin-email:creativembhoni@gmail.com}")
    private String adminEmail;

    public NotificationServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendNewUserAlert(String username, String email, String tenantName) {
        String title = "New User Registration Alert";
        String subtitle = "A new user account has been registered in the platform.";
        
        StringBuilder body = new StringBuilder();
        body.append("<p>A new user account was recently created and requires your review:</p>");
        body.append("<table style='width:100%; border-collapse:collapse; margin:16px 0; font-size:14px;'>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Username:</strong></td><td style='padding:8px 0;'>").append(username).append("</td></tr>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Email:</strong></td><td style='padding:8px 0;'>").append(email).append("</td></tr>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Tenant Workspace:</strong></td><td style='padding:8px 0;'>").append(tenantName != null ? tenantName : "Global Platform").append("</td></tr>");
        body.append("</table>");
        body.append("<p style='color:#575e49; font-size:13px;'>Please log in to the admin console to manage user roles and security permissions.</p>");

        String html = buildBrandedEmailHtml(title, subtitle, "Administrator", body.toString(), null, null, tenantName);
        sendHtmlEmail(adminEmail, "New User Created - " + (tenantName != null ? tenantName : "System"), html);
    }

    @Override
    public void sendRegistrationEmail(String username, String email, String resetLink, String tenantName) {
        String title = "Activate Your Workspace Account";
        String subtitle = "Welcome to Mbhoni Enterprise Suite";
        
        StringBuilder body = new StringBuilder();
        body.append("<p>Your user account has been created for workspace <strong>").append(tenantName).append("</strong>.</p>");
        body.append("<table style='width:100%; border-collapse:collapse; margin:16px 0; font-size:14px;'>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Username:</strong></td><td style='padding:8px 0;'>").append(username).append("</td></tr>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Target Organization:</strong></td><td style='padding:8px 0;'>").append(tenantName).append("</td></tr>");
        body.append("</table>");
        body.append("<p>To complete your registration, set your password, and access your workspace dashboard, please click the secure link below:</p>");
        body.append("<p style='background-color:#fff8f5; border-left:4px solid #713519; padding:12px; font-size:12px; color:#575e49;'><strong>Security Notice:</strong> This activation link expires in 24 hours.</p>");

        String html = buildBrandedEmailHtml(title, subtitle, username, body.toString(), "Set Password & Activate Account", resetLink, tenantName);
        sendHtmlEmail(email, "Welcome to Mbhoni-ES - Set Your Password", html);
    }

    @Override
    public void sendWelcomeEmail(String username, String email, String tenantName) {
        String title = "Welcome to Mbhoni Enterprise Suite";
        String subtitle = "Your workspace account is fully active";

        StringBuilder body = new StringBuilder();
        body.append("<p>We are excited to welcome you to the platform!</p>");
        body.append("<table style='width:100%; border-collapse:collapse; margin:16px 0; font-size:14px;'>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Username:</strong></td><td style='padding:8px 0;'>").append(username).append("</td></tr>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Tenant Workspace:</strong></td><td style='padding:8px 0;'>").append(tenantName).append("</td></tr>");
        body.append("</table>");
        body.append("<p>You can now log in to access your enterprise dashboard, HCM directory, contracts, and business modules.</p>");

        String html = buildBrandedEmailHtml(title, subtitle, username, body.toString(), null, null, tenantName);
        sendHtmlEmail(email, "Welcome to Mbhoni-ES - Account Active", html);
    }

    @Override
    public void sendPasswordResetAlert(String username, String email) {
        String title = "Password Reset Confirmation";
        String subtitle = "Your security credentials have been updated";

        StringBuilder body = new StringBuilder();
        body.append("<p>This email confirms that your account password was successfully updated.</p>");
        body.append("<div style='background-color:#fff3cd; border:1px solid #ffe69c; border-radius:6px; padding:14px; margin:16px 0; font-size:13px; color:#664d03;'>");
        body.append("<strong>Important Security Warning:</strong> If you did not perform or authorize this password reset, please contact your System Administrator immediately to secure your account.");
        body.append("</div>");

        String html = buildBrandedEmailHtml(title, subtitle, username, body.toString(), null, null, null);
        sendHtmlEmail(email, "Password Reset Confirmation", html);
    }

    @Override
    public void sendInvoiceOverdueAlert(String tenantName, String invoiceNumber, double amount, String dueDate) {
        String title = "Commercial Invoice Overdue Alert";
        String subtitle = "Immediate billing follow-up required";

        StringBuilder body = new StringBuilder();
        body.append("<p>An outstanding commercial subscription invoice requires payment follow-up:</p>");
        body.append("<table style='width:100%; border-collapse:collapse; margin:16px 0; font-size:14px;'>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Tenant:</strong></td><td style='padding:8px 0;'>").append(tenantName).append("</td></tr>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Invoice Number:</strong></td><td style='padding:8px 0; font-family:monospace;'>").append(invoiceNumber).append("</td></tr>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Overdue Amount:</strong></td><td style='padding:8px 0; font-weight:700; color:#991b1b;'>R ").append(String.format("%.2f", amount)).append("</td></tr>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Due Date:</strong></td><td style='padding:8px 0;'>").append(dueDate).append("</td></tr>");
        body.append("</table>");

        String html = buildBrandedEmailHtml(title, subtitle, "Billing Administrator", body.toString(), null, null, tenantName);
        sendHtmlEmail(adminEmail, "Invoice Overdue Alert - " + tenantName, html);
    }

    @Override
    public void sendTenantSuspensionAlert(String tenantName, String reason) {
        String title = "Tenant Workspace Suspended";
        String subtitle = "Administrative workspace status update";

        StringBuilder body = new StringBuilder();
        body.append("<p>The following tenant organization workspace has been suspended:</p>");
        body.append("<table style='width:100%; border-collapse:collapse; margin:16px 0; font-size:14px;'>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Tenant Name:</strong></td><td style='padding:8px 0;'>").append(tenantName).append("</td></tr>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Suspension Reason:</strong></td><td style='padding:8px 0; color:#991b1b;'>").append(reason).append("</td></tr>");
        body.append("</table>");

        String html = buildBrandedEmailHtml(title, subtitle, "Administrator", body.toString(), null, null, tenantName);
        sendHtmlEmail(adminEmail, "Tenant Suspended - " + tenantName, html);
    }

    @Override
    public void sendSubscriptionChangeAlert(String tenantName, String oldPlan, String newPlan) {
        String title = "Subscription Plan Modified";
        String subtitle = "Commercial contract tier updated";

        StringBuilder body = new StringBuilder();
        body.append("<p>A tenant subscription tier change has been processed:</p>");
        body.append("<table style='width:100%; border-collapse:collapse; margin:16px 0; font-size:14px;'>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Tenant Name:</strong></td><td style='padding:8px 0;'>").append(tenantName).append("</td></tr>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>Previous Plan:</strong></td><td style='padding:8px 0;'>").append(oldPlan).append("</td></tr>");
        body.append("<tr style='border-bottom:1px solid #efeee8;'><td style='padding:8px 0; color:#575e49;'><strong>New Plan Tier:</strong></td><td style='padding:8px 0; font-weight:700; color:#454f2c;'>").append(newPlan).append("</td></tr>");
        body.append("</table>");

        String html = buildBrandedEmailHtml(title, subtitle, "Administrator", body.toString(), null, null, tenantName);
        sendHtmlEmail(adminEmail, "Subscription Changed - " + tenantName, html);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Branded HTML email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send branded HTML email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String buildBrandedEmailHtml(
            String title,
            String subtitle,
            String recipientName,
            String contentHtml,
            String actionButtonLabel,
            String actionButtonUrl,
            String tenantName) {

        String primaryColor = "#713519";
        String darkNavBg = "#262a1d";
        String badgeText = (tenantName != null && !tenantName.isBlank()) ? tenantName : "System Administration";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        sb.append("<title>").append(title).append("</title></head>");
        sb.append("<body style='margin:0; padding:0; background-color:#f4f3ef; font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,Arial,sans-serif; color:#1c2016;'>");
        
        sb.append("<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='background-color:#f4f3ef; padding:30px 15px;'>");
        sb.append("<tr><td align='center'>");
        sb.append("<table role='presentation' width='100%' style='max-width:600px; background-color:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 4px 16px rgba(0,0,0,0.06); border:1px solid #e8e6dc;'>");

        // Branded Navigation Header
        sb.append("<tr><td style='background-color:").append(darkNavBg).append("; padding:24px 30px; text-align:left;'>");
        sb.append("<table role='presentation' width='100%'><tr>");
        sb.append("<td>");
        sb.append("<h1 style='margin:0; font-size:20px; font-weight:800; color:#ffffff; letter-spacing:0.5px;'>MBHONI <span style='color:#dfddd1; font-weight:400;'>Enterprise Suite</span></h1>");
        sb.append("<p style='margin:4px 0 0 0; font-size:12px; color:#a39f8e;'>Automated Workspace System Notification</p>");
        sb.append("</td>");
        sb.append("<td align='right'>");
        sb.append("<span style='background-color:rgba(255,255,255,0.15); color:#ffffff; font-size:11px; font-weight:600; padding:4px 10px; border-radius:12px;'>").append(badgeText).append("</span>");
        sb.append("</td>");
        sb.append("</tr></table>");
        sb.append("</td></tr>");

        // Title Banner
        sb.append("<tr><td style='padding:25px 30px 10px 30px; border-bottom:1px solid #efeee8;'>");
        sb.append("<h2 style='margin:0; font-size:18px; font-weight:700; color:#1c2016;'>").append(title).append("</h2>");
        if (subtitle != null && !subtitle.isBlank()) {
            sb.append("<p style='margin:6px 0 0 0; font-size:14px; color:#575e49;'>").append(subtitle).append("</p>");
        }
        sb.append("</td></tr>");

        // Main Content Area
        sb.append("<tr><td style='padding:25px 30px;'>");
        if (recipientName != null && !recipientName.isBlank()) {
            sb.append("<p style='margin:0 0 16px 0; font-size:15px; font-weight:600;'>Hello ").append(recipientName).append(",</p>");
        }
        sb.append("<div style='font-size:14px; line-height:1.6; color:#262a1d;'>");
        sb.append(contentHtml);
        sb.append("</div>");

        // Styled Action Button
        if (actionButtonLabel != null && actionButtonUrl != null) {
            sb.append("<table role='presentation' cellspacing='0' cellpadding='0' style='margin:25px 0 10px 0;'><tr><td>");
            sb.append("<a href='").append(actionButtonUrl).append("' target='_blank' style='background-color:").append(primaryColor).append("; color:#ffffff; text-decoration:none; padding:12px 24px; border-radius:6px; font-weight:600; font-size:14px; display:inline-block; box-shadow:0 2px 4px rgba(113,53,25,0.2);'>")
                    .append(actionButtonLabel).append(" &rarr;</a>");
            sb.append("</td></tr></table>");
        }
        sb.append("</td></tr>");

        // Footer
        sb.append("<tr><td style='background-color:#faf9f5; padding:20px 30px; border-top:1px solid #efeee8; text-align:center;'>");
        sb.append("<p style='margin:0 0 6px 0; font-size:12px; color:#575e49;'>This is an automated notification from <strong>Mbhoni Enterprise Suite</strong>.</p>");
        sb.append("<p style='margin:0; font-size:11px; color:#888f7b;'>&copy; 2026 Mbhoni Creative (Pty) Ltd. All rights reserved. | <a href='mailto:").append(fromEmail).append("' style='color:").append(primaryColor).append("; text-decoration:none;'>Contact Support</a></p>");
        sb.append("</td></tr>");

        sb.append("</table></td></tr></table>");
        sb.append("</body></html>");

        return sb.toString();
    }
}
