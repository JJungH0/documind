package com.documind.domain.document.service;

import com.documind.global.config.StorageProperties;
import com.documind.global.exception.BusinessException;
import com.documind.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class FileStorage {

    private static final DateTimeFormatter DATE_PATH =
            DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final Path root;

    public FileStorage(StorageProperties properties) {
        this.root = Path.of(properties.location()).toAbsolutePath().normalize();
    }

    public String calculateHash(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(in, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {

                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED, e);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    public String store(MultipartFile file) {
        String relativePath = LocalDateTime.now().format(DATE_PATH)
                + "/" + UUID.randomUUID() + ".pdf";
        Path target = root.resolve(relativePath);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return relativePath;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED, e);
        }
    }

    public Path resolve(String relativePath) {
        return root.resolve(relativePath).normalize();
    }
}
