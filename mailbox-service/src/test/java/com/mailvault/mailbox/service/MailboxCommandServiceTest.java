package com.mailvault.mailbox.service;

import com.mailvault.mailbox.repository.EmailMessageRepository;
import com.mailvault.mailbox.repository.ThreadLabelRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.List;

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
        when(emailMessageRepository.findThreadFolder(threadId, "user-123")).thenReturn("INBOX");

        var response = mailboxCommandService.markThreadRead("user-123", threadId);

        assertThat(response.status()).isEqualTo("READ");
        assertThat(response.unreadCount()).isZero();
        assertThat(response.folder()).isEqualTo("INBOX");
        verify(emailMessageRepository).markInboundMessagesRead(threadId);
        verify(emailMessageRepository).updateThreadUnreadCount(threadId, "user-123", 0);
    }

    @Test
    void markThreadUnreadMarksLatestInboundMessageAndSetsUnreadCount() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);
        when(emailMessageRepository.markLatestInboundMessageUnread(threadId)).thenReturn(true);
        when(emailMessageRepository.findThreadFolder(threadId, "user-123")).thenReturn("INBOX");

        var response = mailboxCommandService.markThreadUnread("user-123", threadId);

        assertThat(response.status()).isEqualTo("UNREAD");
        assertThat(response.unreadCount()).isEqualTo(1);
        assertThat(response.folder()).isEqualTo("INBOX");
        verify(emailMessageRepository).updateThreadUnreadCount(threadId, "user-123", 1);
    }

    @Test
    void markThreadUnreadHandlesThreadsWithoutInboundMessages() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);
        when(emailMessageRepository.markLatestInboundMessageUnread(threadId)).thenReturn(false);
        when(emailMessageRepository.findThreadFolder(threadId, "user-123")).thenReturn("ACTIVE");

        var response = mailboxCommandService.markThreadUnread("user-123", threadId);

        assertThat(response.status()).isEqualTo("NO_INBOUND_MESSAGE");
        assertThat(response.unreadCount()).isZero();
        assertThat(response.folder()).isEqualTo("ACTIVE");
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

    @Test
    void archiveThreadMovesThreadToArchive() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);
        when(emailMessageRepository.recalculateUnreadCount(threadId)).thenReturn(2);

        var response = mailboxCommandService.archiveThread("user-123", threadId);

        assertThat(response.status()).isEqualTo("ARCHIVED");
        assertThat(response.folder()).isEqualTo("ARCHIVE");
        assertThat(response.unreadCount()).isEqualTo(2);
        verify(emailMessageRepository).updateThreadFolder(threadId, "user-123", "ARCHIVE");
    }

    @Test
    void trashThreadMovesThreadToTrash() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);
        when(emailMessageRepository.recalculateUnreadCount(threadId)).thenReturn(1);

        var response = mailboxCommandService.trashThread("user-123", threadId);

        assertThat(response.status()).isEqualTo("TRASHED");
        assertThat(response.folder()).isEqualTo("TRASH");
        assertThat(response.unreadCount()).isEqualTo(1);
        verify(emailMessageRepository).updateThreadFolder(threadId, "user-123", "TRASH");
    }

    @Test
    void spamThreadMovesThreadToSpam() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);
        when(emailMessageRepository.recalculateUnreadCount(threadId)).thenReturn(1);

        var response = mailboxCommandService.spamThread("user-123", threadId);

        assertThat(response.status()).isEqualTo("SPAMMED");
        assertThat(response.folder()).isEqualTo("SPAM");
        assertThat(response.unreadCount()).isEqualTo(1);
        verify(emailMessageRepository).updateThreadFolder(threadId, "user-123", "SPAM");
    }

    @Test
    void restoreThreadRestoresToRepositoryResolvedFolder() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);
        when(emailMessageRepository.restoreThread(threadId, "user-123")).thenReturn("INBOX");
        when(emailMessageRepository.recalculateUnreadCount(threadId)).thenReturn(1);

        var response = mailboxCommandService.restoreThread("user-123", threadId);

        assertThat(response.status()).isEqualTo("RESTORED");
        assertThat(response.folder()).isEqualTo("INBOX");
        assertThat(response.unreadCount()).isEqualTo(1);
        verify(emailMessageRepository).updateThreadUnreadCount(threadId, "user-123", 1);
    }

    @Test
    void addThreadLabelNormalizesAndReturnsCurrentLabels() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);
        when(emailMessageRepository.findThreadLabels(threadId, "user-123"))
                .thenReturn(List.of(
                        new ThreadLabelRow("FAVORITE", "USER", null),
                        new ThreadLabelRow("IMPORTANT", "USER", null)
                ));

        var response = mailboxCommandService.addThreadLabel("user-123", threadId, "important");

        assertThat(response.status()).isEqualTo("LABEL_ADDED");
        assertThat(response.label()).isEqualTo("IMPORTANT");
        assertThat(response.source()).isEqualTo("USER");
        assertThat(response.labels()).extracting("label").containsExactly("FAVORITE", "IMPORTANT");
        assertThat(response.labels()).extracting("source").containsExactly("USER", "USER");
        verify(emailMessageRepository).addThreadLabel(threadId, "user-123", "IMPORTANT");
    }

    @Test
    void removeThreadLabelNormalizesAndReturnsCurrentLabels() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);
        when(emailMessageRepository.findThreadLabels(threadId, "user-123"))
                .thenReturn(List.of(new ThreadLabelRow("IMPORTANT", "SYSTEM", null)));

        var response = mailboxCommandService.removeThreadLabel("user-123", threadId, "favorite");

        assertThat(response.status()).isEqualTo("LABEL_REMOVED");
        assertThat(response.label()).isEqualTo("FAVORITE");
        assertThat(response.source()).isEqualTo("USER");
        assertThat(response.labels()).extracting("label").containsExactly("IMPORTANT");
        assertThat(response.labels()).extracting("source").containsExactly("SYSTEM");
        verify(emailMessageRepository).removeThreadLabel(threadId, "user-123", "FAVORITE");
    }

    @Test
    void addThreadLabelRejectsInvalidLabel() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.threadExists(threadId, "user-123")).thenReturn(true);

        assertThatThrownBy(() -> mailboxCommandService.addThreadLabel("user-123", threadId, "needs review"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        verify(emailMessageRepository, never()).addThreadLabel(threadId, "user-123", "NEEDS REVIEW");
    }
}
