package com.relyon.parkhere.service;

import com.relyon.parkhere.exception.ImageNotFoundException;
import com.relyon.parkhere.exception.InvalidImageException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "parkhere.images.storage", havingValue = "local", matchIfMissing = true)
public class LocalImageStorageService implements ImageStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    @Value("${parkhere.images.upload-dir:./uploads}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    void init() {
        uploadPath = Path.of(uploadDir);
        try {
            Files.createDirectories(uploadPath);
            log.info("Image upload directory initialized at {}", uploadPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadPath, e);
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        var contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            log.warn("Rejected image upload with content type: {}", contentType);
            throw new InvalidImageException();
        }

        var originalFilename = file.getOriginalFilename();
        var extension = extractExtension(originalFilename);
        var filename = UUID.randomUUID() + extension;

        var targetPath = uploadPath.resolve(filename).normalize();
        if (!targetPath.startsWith(uploadPath)) {
            throw new InvalidImageException();
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored image {} (original: {}, type: {}, size: {} bytes)",
                    filename, originalFilename, contentType, file.getSize());
            return filename;
        } catch (IOException e) {
            log.error("Failed to store image {}: {}", originalFilename, e.getMessage());
            throw new RuntimeException("Failed to store image", e);
        }
    }

    @Override
    public Resource load(String filename) {
        try {
            var path = uploadPath.resolve(filename).normalize();
            if (!path.startsWith(uploadPath)) {
                throw new ImageNotFoundException(filename);
            }
            var resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Image not found: {}", filename);
                throw new ImageNotFoundException(filename);
            }
            log.debug("Loading image: {}", filename);
            return resource;
        } catch (MalformedURLException e) {
            log.error("Malformed URL for image: {}", filename);
            throw new ImageNotFoundException(filename);
        }
    }

    @Override
    public void delete(String filename) {
        var path = uploadPath.resolve(filename).normalize();
        if (!path.startsWith(uploadPath)) {
            return;
        }
        try {
            if (Files.deleteIfExists(path)) {
                log.info("Deleted image: {}", filename);
            } else {
                log.warn("Image not found for deletion: {}", filename);
            }
        } catch (IOException e) {
            log.error("Failed to delete image {}: {}", filename, e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }
}
