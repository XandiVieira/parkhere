package com.relyon.parkhere.exception;

public class TooManyImagesException extends DomainException {

    public TooManyImagesException() {
        super("image.too.many");
    }
}
