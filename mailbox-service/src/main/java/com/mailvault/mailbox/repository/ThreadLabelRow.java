package com.mailvault.mailbox.repository;

import java.math.BigDecimal;

public record ThreadLabelRow(
        String label,
        String source,
        BigDecimal confidenceScore
) {
}
