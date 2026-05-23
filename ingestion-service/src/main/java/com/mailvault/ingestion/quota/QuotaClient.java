package com.mailvault.ingestion.quota;

public interface QuotaClient {

    void reserve(String userId, long bytes);
}
