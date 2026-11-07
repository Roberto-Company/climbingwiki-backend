package com.roberto.climbingwiki.services.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(OutputCaptureExtension.class)
class AccountMailerTest {

    private static final String FRONTEND_URL = "https://climbingwiki.example";
    private static final String FROM = "ClimbingWiki <no-reply@climbingwiki.example>";

    private final JavaMailSender sender = mock(JavaMailSender.class);

    private SimpleMailMessage sentMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void sendPasswordReset_withSmtp_mailsResetLinkToUser() {
        AccountMailer mailer = new AccountMailer(sender, FRONTEND_URL, FROM, false);

        mailer.sendPasswordReset("alice@test.com", "alice", "tok-123");

        SimpleMailMessage message = sentMessage();
        assertThat(message.getTo()).containsExactly("alice@test.com");
        assertThat(message.getFrom()).isEqualTo(FROM);
        assertThat(message.getText())
                .contains("alice")
                .contains(FRONTEND_URL + "/reset-password?token=tok-123");
    }

    @Test
    void sendEmailVerification_withSmtp_mailsVerifyLinkToUser() {
        AccountMailer mailer = new AccountMailer(sender, FRONTEND_URL, FROM, false);

        mailer.sendEmailVerification("alice@test.com", "alice", "tok-456");

        assertThat(sentMessage().getText()).contains(FRONTEND_URL + "/verify-email?token=tok-456");
    }

    @Test
    void links_trailingSlashInFrontendUrl_isNotDoubled() {
        AccountMailer mailer = new AccountMailer(sender, FRONTEND_URL + "/", FROM, false);

        mailer.sendPasswordReset("alice@test.com", "alice", "tok-123");

        assertThat(sentMessage().getText()).contains(FRONTEND_URL + "/reset-password?token=tok-123");
    }

    @Test
    void send_smtpFailure_isLoggedNotThrown() {
        doThrow(new MailSendException("smtp down")).when(sender).send(any(SimpleMailMessage.class));
        AccountMailer mailer = new AccountMailer(sender, FRONTEND_URL, FROM, false);

        assertThatCode(() -> mailer.sendPasswordReset("alice@test.com", "alice", "tok-123"))
                .doesNotThrowAnyException();
    }

    @Test
    void send_withoutSmtp_byDefault_neverLogsTheLink(CapturedOutput output) {
        AccountMailer mailer = new AccountMailer((JavaMailSender) null, FRONTEND_URL, FROM, false);

        assertThatCode(() -> mailer.sendPasswordReset("alice@test.com", "alice", "tok-secret"))
                .doesNotThrowAnyException();
        assertThat(mailer.isSmtpConfigured()).isFalse();
        assertThat(output).doesNotContain("tok-secret");
    }

    @Test
    void send_withoutSmtp_logLinksEnabled_logsTheLinkForLocalDev(CapturedOutput output) {
        AccountMailer mailer = new AccountMailer((JavaMailSender) null, FRONTEND_URL, FROM, true);

        mailer.sendPasswordReset("alice@test.com", "alice", "tok-dev");

        assertThat(output).contains(FRONTEND_URL + "/reset-password?token=tok-dev");
    }
}
