package com.mailvault.mailbox.service;

import com.mailvault.mailbox.repository.EmailMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailboxCommandServiceTest {

    @Mock
    private EmailMessageRepository emailMessageRepository;

    @InjectMocks
    private MailboxCommandService mailboxCommandService;

    @Test
    void markThreadReadMarksInboundMessagesAndClearsUnreadCount() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);

        var response = mailboxCommandService.markThreadRead("user-123", threadId);

        assertThat(response.status()).isEqualTo("READ");
        assertThat(response.unreadCount()).isZero();
        verify(emailMessageRepository).markInboundMessagesRead(threadId);
        verify(emailMessageRepository).updateThreadUnreadCount(threadId, "user-123", 0);
    }

    @Test
    void markThreadUnreadMarksLatestInboundMessageAndSetsUnreadCount() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);
        when(emailMessageRepository.markLatestInboundMessageUnread(threadId)).thenReturn(true);

        var response = mailboxCommandService.markThreadUnread("user-123", threadId);

        assertThat(response.status()).isEqualTo("UNREAD");
        assertThat(response.unreadCount()).isEqualTo(1);
        verify(emailMessageRepository).updateThreadUnreadCount(threadId, "user-123", 1);
    }

    @Test
    void markThreadUnreadHandlesThreadsWithoutInboundMessages() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);
        when(emailMessageRepository.markLatestInboundMessageUnread(threadId)).thenReturn(false);

        var response = mailboxCommandService.markThreadUnread("user-123", threadId);

        assertThat(response.status()).isEqualTo("NO_INBOUND_MESSAGE");
        assertThat(response.unreadCount()).isZero();
        verify(emailMessageRepository).updateThreadUnreadCount(threadId, "user-123", 0);
    }

    @Test
    void markThreadReadRejectsMissingThread() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(false);

        assertThatThrownBy(() -> mailboxCommandService.markThreadRead("user-123", threadId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verify(emailMessageRepository, never()).markInboundMessagesRead(threadId);
    }
}
