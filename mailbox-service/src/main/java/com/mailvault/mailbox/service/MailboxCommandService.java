package com.mailvault.mailbox.service;

import com.mailvault.mailbox.api.ThreadActionResponse;
import com.mailvault.mailbox.api.ThreadLabelActionResponse;
import com.mailvault.mailbox.repository.EmailMessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class MailboxCommandService {

    private static final Pattern LABEL_PATTERN = Pattern.compile("[A-Z0-9_-]{1,40}");

    private final EmailMessageRepository emailMessageRepository;

    public MailboxCommandService(EmailMessageRepository emailMessageRepository) {
        this.emailMessageRepository = emailMessageRepository;
    }

    @Transactional
    public ThreadActionResponse markThreadRead(String userId, UUID threadId) {
        ensureThreadExists(userId, threadId);
        emailMessageRepository.markInboundMessagesRead(threadId);
        emailMessageRepository.updateThreadUnreadCount(threadId, userId, 0);
        String folder = emailMessageRepository.findThreadFolder(threadId, userId);
        return new ThreadActionResponse(threadId, "READ", 0, folder);
    }

    @Transactional
    public ThreadActionResponse markThreadUnread(String userId, UUID threadId) {
        ensureThreadExists(userId, threadId);
        boolean updated = emailMessageRepository.markLatestInboundMessageUnread(threadId);
        int unreadCount = updated ? 1 : 0;
        emailMessageRepository.updateThreadUnreadCount(threadId, userId, unreadCount);
        String folder = emailMessageRepository.findThreadFolder(threadId, userId);
        return new ThreadActionResponse(threadId, updated ? "UNREAD" : "NO_INBOUND_MESSAGE", unreadCount, folder);
    }

    @Transactional
    public ThreadActionResponse archiveThread(String userId, UUID threadId) {
        return moveThread(userId, threadId, "ARCHIVE", "ARCHIVED");
    }

    @Transactional
    public ThreadActionResponse trashThread(String userId, UUID threadId) {
        return moveThread(userId, threadId, "TRASH", "TRASHED");
    }

    @Transactional
    public ThreadActionResponse spamThread(String userId, UUID threadId) {
        return moveThread(userId, threadId, "SPAM", "SPAMMED");
    }

    @Transactional
    public ThreadActionResponse restoreThread(String userId, UUID threadId) {
        ensureThreadExists(userId, threadId);
        String restoredFolder = emailMessageRepository.restoreThread(threadId, userId);
        int unreadCount = emailMessageRepository.recalculateUnreadCount(threadId);
        emailMessageRepository.updateThreadUnreadCount(threadId, userId, unreadCount);
        return new ThreadActionResponse(threadId, "RESTORED", unreadCount, restoredFolder);
    }

    @Transactional
    public ThreadLabelActionResponse addThreadLabel(String userId, UUID threadId, String label) {
        ensureThreadExists(userId, threadId);
        String normalizedLabel = normalizeLabel(label);
        emailMessageRepository.addThreadLabel(threadId, userId, normalizedLabel);
        List<String> labels = emailMessageRepository.findThreadLabels(threadId, userId);
        return new ThreadLabelActionResponse(threadId, "LABEL_ADDED", normalizedLabel, labels);
    }

    @Transactional
    public ThreadLabelActionResponse removeThreadLabel(String userId, UUID threadId, String label) {
        ensureThreadExists(userId, threadId);
        String normalizedLabel = normalizeLabel(label);
        emailMessageRepository.removeThreadLabel(threadId, userId, normalizedLabel);
        List<String> labels = emailMessageRepository.findThreadLabels(threadId, userId);
        return new ThreadLabelActionResponse(threadId, "LABEL_REMOVED", normalizedLabel, labels);
    }

    private void ensureThreadExists(String userId, UUID threadId) {
        if (!emailMessageRepository.threadExists(threadId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "thread not found");
        }
    }

    private ThreadActionResponse moveThread(String userId, UUID threadId, String folder, String status) {
        ensureThreadExists(userId, threadId);
        emailMessageRepository.updateThreadFolder(threadId, userId, folder);
        int unreadCount = emailMessageRepository.recalculateUnreadCount(threadId);
        return new ThreadActionResponse(threadId, status, unreadCount, folder);
    }

    private String normalizeLabel(String label) {
        String normalized = label.trim().toUpperCase(Locale.ROOT);
        if (!LABEL_PATTERN.matcher(normalized).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "label must be 1-40 characters using letters, numbers, underscore, or hyphen"
            );
        }
        return normalized;
    }
}
