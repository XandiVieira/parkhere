package com.relyon.parkhere.exception;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("auth.invalid.credentials");
    }
}
