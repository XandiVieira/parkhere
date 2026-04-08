package com.relyon.parkhere.exception;

public class InvalidResetTokenException extends DomainException {

    public InvalidResetTokenException() {
        super("reset.token.invalid");
    }
}
