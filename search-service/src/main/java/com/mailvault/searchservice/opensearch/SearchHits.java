package com.mailvault.searchservice.opensearch;

import java.util.List;

record SearchHits(
        List<SearchHit> hits
) {
}
