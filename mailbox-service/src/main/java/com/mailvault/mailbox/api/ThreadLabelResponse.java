package com.mailvault.mailbox.api;

import java.math.BigDecimal;

public record ThreadLabelResponse(
        String label,
        String source,
        BigDecimal confidenceScore
) {
}
