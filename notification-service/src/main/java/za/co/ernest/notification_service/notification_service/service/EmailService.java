package za.co.ernest.notification_service.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@quickbite.co.za}")
    private String fromAddress;

    public void sendAccountVerificationEmail(String toEmail, String firstName, String otp) {
        String subject = "QuickBite - Verify Your Account";
        String htmlBody = """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto;">
                    <h2>Welcome to QuickBite, %s!</h2>
                    <p>Use the verification code below to activate your account:</p>
                    <div style="font-size: 28px; font-weight: bold; letter-spacing: 6px; padding: 14px; background: #f4f4f4; text-align: center; border-radius: 6px;">
                        %s
                    </div>
                    <p style="color: #666; font-size: 13px;">This code expires in 5 minutes. If you did not create an account, please ignore this email.</p>
                </div>
                """.formatted(firstName, otp);

        sendHtmlEmail(toEmail, subject, htmlBody);
    }

    public void sendPasswordResetEmail(String toEmail, String firstName, String otp) {
        String subject = "QuickBite - Password Reset Request";
        String htmlBody = """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto;">
                    <h2>Hi %s,</h2>
                    <p>We received a request to reset your QuickBite password. Enter the code below to continue:</p>
                    <div style="font-size: 28px; font-weight: bold; letter-spacing: 6px; padding: 14px; background: #f4f4f4; text-align: center; border-radius: 6px;">
                        %s
                    </div>
                    <p style="color: #666; font-size: 13px;">This code expires in 5 minutes. If you did not request a password reset, please secure your account immediately.</p>
                </div>
                """.formatted(firstName, otp);

        sendHtmlEmail(toEmail, subject, htmlBody);
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Dispatched email [{}] to [{}]", subject, toEmail);
        } catch (MessagingException e) {
            log.error("Failed to build email [{}] for [{}]: {}", subject, toEmail, e.getMessage(), e);
            throw new IllegalStateException("Email composition failed for recipient: " + toEmail, e);
        }
    }
}
