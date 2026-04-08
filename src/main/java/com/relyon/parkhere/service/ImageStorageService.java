package com.relyon.parkhere.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    String store(MultipartFile file);

    Resource load(String filename);

    void delete(String filename);
}
