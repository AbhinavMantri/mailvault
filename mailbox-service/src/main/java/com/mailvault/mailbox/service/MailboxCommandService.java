package com.mailvault.mailbox.service;

import com.mailvault.mailbox.api.ThreadActionResponse;
import com.mailvault.mailbox.repository.EmailMessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class MailboxCommandService {

    private final EmailMessageRepository emailMessageRepository;

    public MailboxCommandService(EmailMessageRepository emailMessageRepository) {
        this.emailMessageRepository = emailMessageRepository;
    }

    @Transactional
    public ThreadActionResponse markThreadRead(String userId, UUID threadId) {
        ensureThreadExists(userId, threadId);
        emailMessageRepository.markInboundMessagesRead(threadId);
        emailMessageRepository.updateThreadUnreadCount(threadId, userId, 0);
        return new ThreadActionResponse(threadId, "READ", 0);
    }

    @Transactional
    public ThreadActionResponse markThreadUnread(String userId, UUID threadId) {
        ensureThreadExists(userId, threadId);
        boolean updated = emailMessageRepository.markLatestInboundMessageUnread(threadId);
        int unreadCount = updated ? 1 : 0;
        emailMessageRepository.updateThreadUnreadCount(threadId, userId, unreadCount);
        return new ThreadActionResponse(threadId, updated ? "UNREAD" : "NO_INBOUND_MESSAGE", unreadCount);
    }

    private void ensureThreadExists(String userId, UUID threadId) {
        if (!emailMessageRepository.threadExists(threadId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "thread not found");
        }
    }
}
