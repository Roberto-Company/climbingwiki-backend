package com.roberto.climbingwiki.services.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Sends account emails (password reset, email verification) with a link back to the site.
 *
 * <p>SMTP is used when Spring Boot has a {@link JavaMailSender}, i.e. when {@code spring.mail.host}
 * is set. Without it nothing is sent; the link is written to the log only when
 * {@code climbingwiki.mail.log-links} is on (local development and the Docker demo), so a
 * deployment that forgets SMTP doesn't leak live tokens into its logs. Sending is asynchronous so
 * response times don't reveal whether an account exists.
 */
@Component
public class AccountMailer {

    private static final Logger log = LoggerFactory.getLogger(AccountMailer.class);

    private static final String RESET_SUBJECT = "ClimbingWiki: restablecer contraseña / reset your password";
    private static final String RESET_TEXT = """
            Hola %1$s,

            Para elegir una contraseña nueva abre este enlace (caduca en 1 hora):
            %2$s

            Si no lo has pedido tú, ignora este correo.

            ---

            Hi %1$s,

            To choose a new password, open this link (it expires in 1 hour):
            %2$s

            If you didn't ask for this, you can ignore this email.
            """;

    private static final String VERIFY_SUBJECT = "ClimbingWiki: confirma tu correo / verify your email";
    private static final String VERIFY_TEXT = """
            Hola %1$s,

            Confirma tu dirección de correo abriendo este enlace (caduca en 24 horas):
            %2$s

            ---

            Hi %1$s,

            Confirm your email address by opening this link (it expires in 24 hours):
            %2$s
            """;

    private final JavaMailSender sender;
    private final String frontendUrl;
    private final String from;
    private final boolean logLinks;

    @Autowired
    public AccountMailer(
            ObjectProvider<JavaMailSender> sender,
            @Value("${climbingwiki.frontend-url}") String frontendUrl,
            @Value("${climbingwiki.mail.from}") String from,
            @Value("${climbingwiki.mail.log-links:false}") boolean logLinks) {
        this(sender.getIfAvailable(), frontendUrl, from, logLinks);
        if (this.sender == null && !logLinks) {
            log.warn("SMTP is not configured (spring.mail.host): password-reset and verification emails "
                    + "will not be sent.");
        }
    }

    AccountMailer(JavaMailSender sender, String frontendUrl, String from, boolean logLinks) {
        this.sender = sender;
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
        this.from = from;
        this.logLinks = logLinks;
    }

    public boolean isSmtpConfigured() {
        return sender != null;
    }

    @Async
    public void sendPasswordReset(String email, String username, String token) {
        String link = link("/reset-password", token);
        send(email, RESET_SUBJECT, RESET_TEXT.formatted(username, link), link);
    }

    @Async
    public void sendEmailVerification(String email, String username, String token) {
        String link = link("/verify-email", token);
        send(email, VERIFY_SUBJECT, VERIFY_TEXT.formatted(username, link), link);
    }

    private String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path(path)
                .queryParam("token", token)
                .encode()
                .toUriString();
    }

    private void send(String to, String subject, String text, String link) {
        if (sender == null) {
            if (logLinks) {
                log.info("SMTP not configured; email to {} not sent. Link: {}", to, link);
            } else {
                log.warn("SMTP not configured; \"{}\" email not sent.", subject);
            }
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            sender.send(message);
        } catch (MailException ex) {
            log.error("Could not send \"{}\" email to {}", subject, to, ex);
        }
    }
}
