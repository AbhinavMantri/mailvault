package com.mailvault.mailbox.api;

import com.mailvault.mailbox.service.MailboxQueryService;
import com.mailvault.mailbox.service.MailboxCommandService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
public class MailboxController {

    private final MailboxQueryService mailboxQueryService;
    private final MailboxCommandService mailboxCommandService;

    public MailboxController(MailboxQueryService mailboxQueryService, MailboxCommandService mailboxCommandService) {
        this.mailboxQueryService = mailboxQueryService;
        this.mailboxCommandService = mailboxCommandService;
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

    @GetMapping("/mailboxes/{userId}/labels/{label}/threads")
    List<ThreadSummaryResponse> threadsByLabel(
            @PathVariable @NotBlank String userId,
            @PathVariable @NotBlank String label,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        return mailboxQueryService.getThreadsByLabel(userId, label, limit);
    }

    @GetMapping("/threads/{threadId}")
    ThreadDetailResponse threadDetail(
            @PathVariable UUID threadId,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxQueryService.getThreadDetail(userId, threadId);
    }

    @PostMapping("/threads/{threadId}/read")
    ThreadActionResponse markThreadRead(
            @PathVariable UUID threadId,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxCommandService.markThreadRead(userId, threadId);
    }

    @PostMapping("/threads/{threadId}/unread")
    ThreadActionResponse markThreadUnread(
            @PathVariable UUID threadId,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxCommandService.markThreadUnread(userId, threadId);
    }

    @PostMapping("/threads/{threadId}/archive")
    ThreadActionResponse archiveThread(
            @PathVariable UUID threadId,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxCommandService.archiveThread(userId, threadId);
    }

    @PostMapping("/threads/{threadId}/trash")
    ThreadActionResponse trashThread(
            @PathVariable UUID threadId,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxCommandService.trashThread(userId, threadId);
    }

    @PostMapping("/threads/{threadId}/spam")
    ThreadActionResponse spamThread(
            @PathVariable UUID threadId,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxCommandService.spamThread(userId, threadId);
    }

    @PostMapping("/threads/{threadId}/restore")
    ThreadActionResponse restoreThread(
            @PathVariable UUID threadId,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxCommandService.restoreThread(userId, threadId);
    }

    @PostMapping("/threads/{threadId}/labels/{label}")
    ThreadLabelActionResponse addThreadLabel(
            @PathVariable UUID threadId,
            @PathVariable @NotBlank String label,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxCommandService.addThreadLabel(userId, threadId, label);
    }

    @DeleteMapping("/threads/{threadId}/labels/{label}")
    ThreadLabelActionResponse removeThreadLabel(
            @PathVariable UUID threadId,
            @PathVariable @NotBlank String label,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxCommandService.removeThreadLabel(userId, threadId, label);
    }

    @GetMapping("/emails/{emailId}")
    EmailDetailResponse emailDetail(
            @PathVariable UUID emailId,
            @RequestParam @NotBlank String userId
    ) {
        return mailboxQueryService.getEmailDetail(userId, emailId);
    }
}
