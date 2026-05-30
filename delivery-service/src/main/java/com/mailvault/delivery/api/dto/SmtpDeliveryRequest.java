package com.mailvault.delivery.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SmtpDeliveryRequest(
        @NotBlank @Email String from,
        @NotEmpty List<@Email String> to,
        List<@Email String> cc,
        List<@Email String> bcc,
        @NotBlank String subject,
        String textBody,
        String htmlBody
) {
}
