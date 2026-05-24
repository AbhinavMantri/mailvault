package com.mailvault.quota.events;

import com.mailvault.quota.service.QuotaUsageService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EmailReceivedConsumer {

    private final QuotaUsageService quotaUsageService;

    public EmailReceivedConsumer(QuotaUsageService quotaUsageService) {
        this.quotaUsageService = quotaUsageService;
    }

    @KafkaListener(
            topics = "${mailvault.kafka.email-received-topic}",
            groupId = "${mailvault.kafka.consumer-group-id}"
    )
    public void onEmailReceived(EmailReceivedEvent event) {
        quotaUsageService.recordEmailReceived(event);
    }
}
