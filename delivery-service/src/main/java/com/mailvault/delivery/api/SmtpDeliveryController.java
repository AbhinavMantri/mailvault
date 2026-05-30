package com.mailvault.delivery.api;

import com.mailvault.delivery.api.dto.DeliveryResponse;
import com.mailvault.delivery.api.dto.SmtpDeliveryRequest;
import com.mailvault.delivery.service.SmtpDeliveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deliveries")
public class SmtpDeliveryController {

    private final SmtpDeliveryService smtpDeliveryService;

    public SmtpDeliveryController(SmtpDeliveryService smtpDeliveryService) {
        this.smtpDeliveryService = smtpDeliveryService;
    }

    @PostMapping("/smtp")
    public DeliveryResponse send(@Valid @RequestBody SmtpDeliveryRequest request) {
        return smtpDeliveryService.send(request);
    }
}
