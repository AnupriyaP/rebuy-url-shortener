package com.rebuy.urlshortener.exception;

public class HashNotFoundException extends RuntimeException {

    public HashNotFoundException(String hash) {
        super("No URL found for hash: " + hash);
    }
}