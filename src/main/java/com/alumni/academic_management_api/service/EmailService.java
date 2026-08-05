package com.alumni.academic_management_api.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String from;
    private final String frontendUrl;

    public EmailService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${mail.from}") String from,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    @Async
    public void sendApprovalEmail(String to, String name) {
        try {
            Context ctx = new Context();
            ctx.setVariable("name", name);
            ctx.setVariable("platformUrl", frontendUrl);
            String html = templateEngine.process("email/approval", ctx);
            sendHtmlEmail(to, "Seu cadastro foi aprovado!", html);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de aprovação para {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendForgotPasswordEmail(String to, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password?token="
                    + URLEncoder.encode(token, StandardCharsets.UTF_8);
            Context ctx = new Context();

            ctx.setVariable("resetLink", resetLink);

            String html = templateEngine.process("email/forgot-password", ctx);

            sendHtmlEmail(to, "Redefinição de senha", html);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de recuperação de senha para {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendAccountApprovalEmail(String to, String name, String rawToken) {
        try {
            String setupUrl = frontendUrl + "/auth/set-password?token="
                    + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
            Context ctx = new Context();
            ctx.setVariable("name", name);
            ctx.setVariable("setupUrl", setupUrl);
            String html = templateEngine.process("email/account-approval", ctx);
            sendHtmlEmail(to, "Seu cadastro foi aprovado — Alumni IFMA", html);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de aprovação de cadastro para {}: {}", to, e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}
