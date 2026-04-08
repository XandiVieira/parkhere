package com.relyon.parkhere.exception;

public class FavoriteNotFoundException extends DomainException {

    public FavoriteNotFoundException() {
        super("favorite.not.found");
    }
}
