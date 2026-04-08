package com.relyon.parkhere.controller;

import com.relyon.parkhere.exception.ImageNotFoundException;
import com.relyon.parkhere.service.ImageStorageService;
import com.relyon.parkhere.service.LocalizedMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageStorageService imageStorageService;
    private final LocalizedMessageService localizedMessageService;

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) throws IOException {
        var resource = imageStorageService.load(filename);
        var contentType = Files.probeContentType(Path.of(filename));
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        log.debug("Serving image: {} with content type: {}", filename, contentType);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleImageNotFound(ImageNotFoundException ex) {
        var message = localizedMessageService.translate(ex);
        log.warn("Image not found: {}", message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "status", HttpStatus.NOT_FOUND.value(),
                        "message", message,
                        "timestamp", LocalDateTime.now()
                ));
    }
}
