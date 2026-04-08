package com.relyon.parkhere.exception;

public class AlreadyFavoritedException extends DomainException {

    public AlreadyFavoritedException() {
        super("favorite.already.exists");
    }
}
