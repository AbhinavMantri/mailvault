package com.mailvault.delivery.service;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.mailvault.delivery.api.dto.SmtpDeliveryRequest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpDeliveryServiceTest {

    private GreenMail greenMail;
    private SmtpDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        greenMail = new GreenMail(new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
        greenMail.start();

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("127.0.0.1");
        mailSender.setPort(greenMail.getSmtp().getPort());
        mailSender.setDefaultEncoding("UTF-8");

        deliveryService = new SmtpDeliveryService(mailSender);
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }

    @Test
    void sendDeliversMessageToGreenMailSmtpServer() throws Exception {
        SmtpDeliveryRequest request = new SmtpDeliveryRequest(
                "sender@mailvault.local",
                List.of("finance@example.com"),
                List.of("manager@example.com"),
                List.of("audit@example.com"),
                "MailVault SMTP test",
                "Plain body",
                "<p>HTML body</p>"
        );

        var response = deliveryService.send(request);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.recipientCount()).isEqualTo(3);
        assertThat(greenMail.waitForIncomingEmail(1)).isTrue();
        MimeMessage message = greenMail.getReceivedMessages()[0];
        assertThat(message.getSubject()).isEqualTo("MailVault SMTP test");
        assertThat(message.getFrom()[0].toString()).isEqualTo("sender@mailvault.local");
    }
}
