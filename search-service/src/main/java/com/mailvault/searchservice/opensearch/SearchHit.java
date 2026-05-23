package com.mailvault.searchservice.opensearch;

import com.fasterxml.jackson.annotation.JsonProperty;

record SearchHit(
        @JsonProperty("_source") SearchDocument source
) {
}
