package za.co.ernest.notification_service.notification_service.service;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;
    private final String fromAddress = "no-reply@quickbite.co.za";
    private final String toEmail = "ernest@example.co.za";
    private final String firstName = "Ernest";
    private final String otp = "482910";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", fromAddress);

        mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("sendAccountVerificationEmail: Should populate headers and HTML body with firstName and OTP, then send")
    void sendAccountVerificationEmail_ValidInputs_SendsFormattedHtmlEmail() throws Exception {

        emailService.sendAccountVerificationEmail(toEmail, firstName, otp);

        verify(mailSender, times(1)).send(mimeMessage);

        assertEquals("QuickBite - Verify Your Account", mimeMessage.getSubject());
        assertEquals(fromAddress, mimeMessage.getFrom()[0].toString());
        assertEquals(toEmail, mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());

        String htmlContent = extractHtmlContent(mimeMessage);
        assertTrue(htmlContent.contains("Welcome to QuickBite, " + firstName + "!"));
        assertTrue(htmlContent.contains(otp));
        assertTrue(htmlContent.contains("activate your account"));
    }

    @Test
    @DisplayName("sendPasswordResetEmail: Should populate headers and HTML body with firstName and OTP, then send")
    void sendPasswordResetEmail_ValidInputs_SendsFormattedHtmlEmail() throws Exception {

        emailService.sendPasswordResetEmail(toEmail, firstName, otp);

        verify(mailSender, times(1)).send(mimeMessage);

        assertEquals("QuickBite - Password Reset Request", mimeMessage.getSubject());
        assertEquals(fromAddress, mimeMessage.getFrom()[0].toString());
        assertEquals(toEmail, mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());

        String htmlContent = extractHtmlContent(mimeMessage);
        assertTrue(htmlContent.contains("Hi " + firstName + ","));
        assertTrue(htmlContent.contains(otp));
        assertTrue(htmlContent.contains("reset your QuickBite password"));
    }

    @Test
    @DisplayName("sendHtmlEmail: Should throw IllegalStateException when MessagingException occurs during composition")
    void sendAccountVerificationEmail_InvalidRecipientAddress_ThrowsIllegalStateException() {

        String invalidEmail = "invalid email address with spaces";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> emailService.sendAccountVerificationEmail(invalidEmail, firstName, otp)
        );

        assertEquals("Email composition failed for recipient: " + invalidEmail, exception.getMessage());
        assertInstanceOf(MessagingException.class, exception.getCause());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("sendHtmlEmail: Should propagate MailException when mail server fails to dispatch")
    void sendAccountVerificationEmail_MailServerFails_ThrowsMailException() {
        doThrow(new MailSendException("SMTP connection timed out"))
                .when(mailSender).send(any(MimeMessage.class));

        MailSendException exception = assertThrows(
                MailSendException.class,
                () -> emailService.sendAccountVerificationEmail(toEmail, firstName, otp)
        );

        assertEquals("SMTP connection timed out", exception.getMessage());
    }

    private String extractHtmlContent(MimeMessage message) throws Exception {
        message.saveChanges();
        Object content = message.getContent();
        if (content instanceof Multipart multipart) {
            return extractTextFromMultipart(multipart);
        }
        return content.toString();
    }

    private String extractTextFromMultipart(Multipart multipart) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.isMimeType("text/html")) {
                return part.getContent().toString();
            } else if (part.getContent() instanceof Multipart nested) {
                String result = extractTextFromMultipart(nested);
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        return "";
    }
}

