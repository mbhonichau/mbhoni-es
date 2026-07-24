package com.mbhoni_creative.adminservice.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.mbhoni_creative.adminservice.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${app.admin-email}")
    private String adminEmail;

    @Override
    public void sendNewUserAlert(String username, String email, String tenantName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(adminEmail);
        message.setSubject("New User Created - " + tenantName);
        message.setText("A new user has been created:\n\n" +
                "Username: " + username + "\n" +
                "Email: " + email + "\n" +
                "Tenant: " + tenantName + "\n\n" +
                "Please review the user details.");
        mailSender.send(message);
    }

    @Override
    public void sendRegistrationEmail(String username, String email, String resetLink, String tenantName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Welcome to Mbhoni-ES - Set Your Password");
        message.setText("Hello " + username + ",\n\n" +
                "Welcome to Mbhoni-ES! Your account has been successfully created.\n\n" +
                "Tenant: " + tenantName + "\n" +
                "Username: " + username + "\n\n" +
                "To set your password and activate your account, please click the link below:\n" +
                resetLink + "\n\n" +
                "This link will expire in 24 hours for security reasons.\n\n" +
                "Regards,\nMbhoni-ES Team");
        mailSender.send(message);
    }

    @Override
    public void sendWelcomeEmail(String username, String email, String tenantName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Welcome to Mbhoni-ES - Account Created");
        message.setText("Hello " + username + ",\n\n" +
                "Welcome to Mbhoni-ES! Your account has been successfully created.\n\n" +
                "Tenant: " + tenantName + "\n" +
                "Username: " + username + "\n\n" +
                "You can now log in to your account using your credentials.\n\n" +
                "Regards,\nMbhoni-ES Team");
        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetAlert(String username, String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Password Reset Confirmation");
        message.setText("Hello " + username + ",\n\n" +
                "Your password has been successfully reset.\n" +
                "If you did not request this change, please contact support immediately.\n\n" +
                "Regards,\nMbhoni-ES Admin Team");
        mailSender.send(message);
    }

    @Override
    public void sendInvoiceOverdueAlert(String tenantName, String invoiceNumber, double amount, String dueDate) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(adminEmail);
        message.setSubject("Invoice Overdue Alert - " + tenantName);
        message.setText("An invoice is overdue:\n\n" +
                "Tenant: " + tenantName + "\n" +
                "Invoice Number: " + invoiceNumber + "\n" +
                "Amount: $" + amount + "\n" +
                "Due Date: " + dueDate + "\n\n" +
                "Please follow up with the tenant for payment.");
        mailSender.send(message);
    }

    @Override
    public void sendTenantSuspensionAlert(String tenantName, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(adminEmail);
        message.setSubject("Tenant Suspended - " + tenantName);
        message.setText("A tenant has been suspended:\n\n" +
                "Tenant: " + tenantName + "\n" +
                "Reason: " + reason + "\n\n" +
                "Please review the suspension details.");
        mailSender.send(message);
    }

    @Override
    public void sendSubscriptionChangeAlert(String tenantName, String oldPlan, String newPlan) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(adminEmail);
        message.setSubject("Subscription Changed - " + tenantName);
        message.setText("A tenant has changed their subscription:\n\n" +
                "Tenant: " + tenantName + "\n" +
                "Previous Plan: " + oldPlan + "\n" +
                "New Plan: " + newPlan + "\n\n" +
                "Please review the subscription change.");
        mailSender.send(message);
    }
}
