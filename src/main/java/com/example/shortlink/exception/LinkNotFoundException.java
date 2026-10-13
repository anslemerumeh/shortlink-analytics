package com.example.shortlink.exception;

public class LinkNotFoundException extends RuntimeException {

    public LinkNotFoundException(String code) {
        super("No short link found for code '" + code + "'");
    }
}
