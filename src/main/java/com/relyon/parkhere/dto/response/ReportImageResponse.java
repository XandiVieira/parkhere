package com.relyon.parkhere.dto.response;

import com.relyon.parkhere.model.ReportImage;

public record ReportImageResponse(String filename, String originalFilename, String contentType) {

    public static ReportImageResponse from(ReportImage image) {
        return new ReportImageResponse(image.getFilename(), image.getOriginalFilename(), image.getContentType());
    }
}
