package com.javarush.taskmanager.exception;

public class TokenNotFoundException extends BusinessException {
    public TokenNotFoundException(String message) {
        super(message);
    }
}
