package com.appdev.Finance.Service;

import java.util.Properties;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailUtil {

    @Value("${spring.mail.username}")
    private String myEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    private JavaMailSender mailSender;


    @Async("asyncMailExecutor")
    public void sendVerificationEmailViaMailSenderAsync(String recipient, String token) {
        System.out.println("🔹 Asynchronously preparing to send verification email to " + recipient + " using JavaMailSender...");

        String subject = "Verify Your Email - Personal Finance Tracker";
        String verificationLink = baseUrl + "/verify?token=" + token;
        // Link to directly resend a new verification email for this recipient
        String directResendLink = baseUrl + "/resend-verification-directly?email=" + recipient; // Using recipient's email

        String emailContent = "<h2>Email Verification</h2>" +
                              "<p>Thank you for registering with Personal Finance Tracker.</p>" +
                              "<p>Please click the link below to verify your email address. This link is valid for 5 minutes:</p>" +
                              "<p><a href='" + verificationLink + "' style='color: #0066cc; font-weight: bold; text-decoration: none;'>Verify Email Address</a></p>" +
                              "<hr>" +
                              "<p style='font-size:0.9em; color:#555;'>If the link above has expired (after 5 minutes), or if you did not receive it properly, you can request a new one by clicking here:</p>" +
                              "<p><a href='" + directResendLink + "' style='color: #0066cc;'>Request a New Verification Link</a></p>" +
                              "<p style='font-size:0.9em; color:#555;'>If you did not register for an account, please ignore this email.</p>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(myEmail);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(emailContent, true);

            mailSender.send(message);
            System.out.println("✅ Verification email task submitted successfully via JavaMailSender for " + recipient);
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send verification email using JavaMailSender: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void sendEmail(String recipient, String subject, String body, boolean isHtml) {
        System.out.println("📧 Attempting to send email via JavaMailSender to: " + recipient);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(myEmail);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(body, isHtml);
            mailSender.send(message);
            System.out.println("✅ Email sent successfully via JavaMailSender to " + recipient);
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email using JavaMailSender: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendResetPasswordEmail(String email, String resetToken) {
        String subject = "Reset Your Password - Personal Finance Tracker";
        String resetLink = baseUrl + "/reset-password?token=" + resetToken;

        String body = "<h2>Password Reset Request</h2>" +
                      "<p>We received a request to reset your password for your Personal Finance Tracker account.</p>" +
                      "<p>Click the link below to set a new password (link valid for 5 minutes):</p>" +
                      "<p><a href='" + resetLink + "' style='color: #0066cc; font-weight: bold; text-decoration: none;'>Reset Password</a></p>" +
                      "<p>If you didn't request this, you can safely ignore this email.</p>";

        sendEmail(email, subject, body, true);
    }


    @Deprecated
    public static void sendVerificationEmail(String recipient, String token) {
        // ... (deprecated method remains unchanged)
    }
}