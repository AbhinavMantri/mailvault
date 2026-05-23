package com.mailvault.searchservice.api;

import com.mailvault.searchservice.service.EmailSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
public class SearchController {

    private final EmailSearchService emailSearchService;

    public SearchController(EmailSearchService emailSearchService) {
        this.emailSearchService = emailSearchService;
    }

    @GetMapping("/emails/search")
    List<SearchResultResponse> search(
            @RequestParam @NotBlank String userId,
            @RequestParam @NotBlank String q,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return emailSearchService.search(userId, q, limit);
    }
}
