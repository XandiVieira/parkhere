package com.relyon.parkhere.exception;

public class ImageNotFoundException extends DomainException {

    public ImageNotFoundException(String filename) {
        super("image.not.found", filename);
    }
}
