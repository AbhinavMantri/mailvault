package com.mailvault.mailbox.api;

import com.mailvault.mailbox.domain.RecipientType;

public record RecipientResponse(
        String address,
        RecipientType type
) {
}
