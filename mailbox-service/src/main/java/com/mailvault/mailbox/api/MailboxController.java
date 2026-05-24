package com.mailvault.mailbox.api;

import com.mailvault.mailbox.service.MailboxQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
public class MailboxController {

    private final MailboxQueryService mailboxQueryService;

    public MailboxController(MailboxQueryService mailboxQueryService) {
        this.mailboxQueryService = mailboxQueryService;
    }

    @GetMapping("/mailboxes/{userId}/inbox")
    List<InboxItemResponse> inbox(
            @PathVariable @NotBlank String userId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return mailboxQueryService.getInbox(userId, limit);
    }

    @GetMapping("/mailboxes/{userId}/threads")
    List<ThreadSummaryResponse> threads(
            @PathVariable @NotBlank String userId,
            @RequestParam(defaultValue = "INBOX") @NotBlank String folder,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return mailboxQueryService.getThreads(userId, folder, limit);
    }

    @GetMapping("/threads/{threadId}")
    ThreadDetailResponse threadDetail(
            @PathVariable UUID threadId,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxQueryService.getThreadDetail(userId, threadId);
    }

    @GetMapping("/emails/{emailId}")
    EmailDetailResponse emailDetail(
            @PathVariable UUID emailId,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxQueryService.getEmailDetail(userId, emailId);
    }
}
