package com.relyon.parkhere.exception;

public class InvalidImageException extends DomainException {

    public InvalidImageException() {
        super("image.invalid.type");
    }
}
