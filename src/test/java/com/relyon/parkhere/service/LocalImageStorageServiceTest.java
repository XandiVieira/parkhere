package com.relyon.parkhere.service;

import com.relyon.parkhere.exception.ImageNotFoundException;
import com.relyon.parkhere.exception.InvalidImageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalImageStorageServiceTest {

    private LocalImageStorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageService = new LocalImageStorageService();
        ReflectionTestUtils.setField(storageService, "uploadDir", tempDir.toString());
        storageService.init();
    }

    @Test
    void store_shouldSaveFileAndReturnFilename() throws IOException {
        var file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", "fake-image-data".getBytes());

        var filename = storageService.store(file);

        assertNotNull(filename);
        assertTrue(filename.endsWith(".jpg"));
        assertTrue(Files.exists(tempDir.resolve(filename)));
    }

    @Test
    void store_shouldRejectInvalidContentType() {
        var file = new MockMultipartFile("image", "document.txt", "text/plain", "not an image".getBytes());

        assertThrows(InvalidImageException.class, () -> storageService.store(file));
    }

    @Test
    void store_shouldGenerateUniqueFilename() {
        var file = new MockMultipartFile("image", "photo.png", "image/png", "fake-image-data".getBytes());

        var filename = storageService.store(file);

        assertNotEquals("photo.png", filename);
    }

    @Test
    void load_shouldReturnResourceForExistingFile() throws IOException {
        var testFile = tempDir.resolve("test-image.jpg");
        Files.write(testFile, "fake-image-data".getBytes());

        var resource = storageService.load("test-image.jpg");

        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void load_shouldThrowForNonExistentFile() {
        assertThrows(ImageNotFoundException.class, () -> storageService.load("nonexistent.jpg"));
    }
}
