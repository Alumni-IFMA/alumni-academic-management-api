package com.alumni.academic_management_api.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, templateEngine, "noreply@test.com", "http://localhost:3000");
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Nested
    class SendApprovalEmail {

        @Test
        void givenValidRecipient_whenSendApprovalEmail_thenCallsMailSenderSend() {
            when(templateEngine.process(eq("email/approval"), any(Context.class)))
                    .thenReturn("<html><body>Aprovado!</body></html>");

            emailService.sendApprovalEmail("joao@email.com", "João");

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        void givenValidRecipient_whenSendApprovalEmail_thenProcessesApprovalTemplate() {
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("email/approval"), contextCaptor.capture()))
                    .thenReturn("<html><body>Aprovado!</body></html>");

            emailService.sendApprovalEmail("joao@email.com", "João");

            Context ctx = contextCaptor.getValue();
            assertThat(ctx.getVariable("name")).isEqualTo("João");
            assertThat(ctx.getVariable("platformUrl")).isEqualTo("http://localhost:3000");
        }

        @Test
        void givenMailSenderThrowsException_whenSendApprovalEmail_thenDoesNotPropagate() {
            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP error"));

            assertThatCode(() -> emailService.sendApprovalEmail("joao@email.com", "João"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class SendForgotPasswordEmail {

        @Test
        void givenValidRecipient_whenSendForgotPasswordEmail_thenCallsMailSenderSend() {
            when(templateEngine.process(eq("email/forgot-password"), any(Context.class)))
                    .thenReturn("<html><body>Reset</body></html>");

            emailService.sendForgotPasswordEmail("joao@email.com", "abc123token");

            verify(mailSender).send(any(MimeMessage.class));
        }

        @Test
        void givenValidRecipient_whenSendForgotPasswordEmail_thenProcessesForgotPasswordTemplate() {
            ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
            when(templateEngine.process(eq("email/forgot-password"), contextCaptor.capture()))
                    .thenReturn("<html><body>Reset</body></html>");

            emailService.sendForgotPasswordEmail("joao@email.com", "abc123token");

            Context ctx = contextCaptor.getValue();
            assertThat(ctx.getVariable("resetLink"))
                    .isEqualTo("http://localhost:3000/reset-password?token=abc123token");
        }

        @Test
        void givenMailSenderThrowsException_whenSendForgotPasswordEmail_thenDoesNotPropagate() {
            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP error"));

            assertThatCode(() -> emailService.sendForgotPasswordEmail("joao@email.com", "abc123token"))
                    .doesNotThrowAnyException();
        }
    }
}
