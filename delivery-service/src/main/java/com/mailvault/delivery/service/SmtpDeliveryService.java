package com.mailvault.delivery.service;

import com.mailvault.delivery.api.dto.DeliveryResponse;
import com.mailvault.delivery.api.dto.SmtpDeliveryRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmtpDeliveryService {

    private final JavaMailSender mailSender;

    public SmtpDeliveryService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public DeliveryResponse send(SmtpDeliveryRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, request.htmlBody() != null, "UTF-8");
            helper.setFrom(request.from());
            helper.setTo(toArray(request.to()));
            if (!safeList(request.cc()).isEmpty()) {
                helper.setCc(toArray(request.cc()));
            }
            if (!safeList(request.bcc()).isEmpty()) {
                helper.setBcc(toArray(request.bcc()));
            }
            helper.setSubject(request.subject());
            if (request.htmlBody() == null) {
                helper.setText(safeText(request.textBody()), false);
            } else {
                helper.setText(safeText(request.textBody()), request.htmlBody());
            }
            mailSender.send(message);
            return new DeliveryResponse("ACCEPTED", recipientCount(request));
        } catch (MessagingException | MailException ex) {
            throw new IllegalStateException("SMTP delivery failed", ex);
        }
    }

    private int recipientCount(SmtpDeliveryRequest request) {
        return safeList(request.to()).size() + safeList(request.cc()).size() + safeList(request.bcc()).size();
    }

    private String[] toArray(List<String> values) {
        return safeList(values).toArray(String[]::new);
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
