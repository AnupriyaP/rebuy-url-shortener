package com.rebuy.urlshortener.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String ip) {
        super("Too many requests from IP: " + ip
                + ". Please wait before trying again.");
    }
}