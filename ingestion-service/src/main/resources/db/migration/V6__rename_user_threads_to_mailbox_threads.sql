ALTER TABLE user_threads RENAME TO mailbox_threads;

ALTER INDEX idx_user_threads_user_folder_last_message
    RENAME TO idx_mailbox_threads_user_folder_last_message;
