package com.mailvault.delivery.api.dto;

public record DeliveryResponse(
        String status,
        int recipientCount
) {
}
