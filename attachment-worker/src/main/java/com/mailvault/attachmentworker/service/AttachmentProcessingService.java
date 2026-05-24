package com.mailvault.attachmentworker.service;

import com.mailvault.attachmentworker.repository.AttachmentRepository;
import com.mailvault.attachmentworker.repository.AttachmentRow;
import com.mailvault.attachmentworker.storage.AttachmentObjectStorage;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class AttachmentProcessingService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentObjectStorage objectStorage;

    public AttachmentProcessingService(
            AttachmentRepository attachmentRepository,
            AttachmentObjectStorage objectStorage
    ) {
        this.attachmentRepository = attachmentRepository;
        this.objectStorage = objectStorage;
    }

    public void process(AttachmentRow attachment) {
        if (!attachmentRepository.markProcessing(attachment.id())) {
            return;
        }

        try {
            byte[] bytes = objectStorage.readBytes(attachment.objectKey());
            String sha256 = sha256(bytes);
            attachmentRepository.markReady(attachment.id(), sha256);
        } catch (RuntimeException exception) {
            attachmentRepository.markFailed(attachment.id());
            throw exception;
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }
}
