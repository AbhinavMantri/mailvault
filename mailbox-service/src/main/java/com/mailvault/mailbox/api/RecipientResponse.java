package com.mailvault.mailbox.api;

public record RecipientResponse(
        String address,
        String type
) {
}
