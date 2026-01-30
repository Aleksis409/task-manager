package com.javarush.taskmanager.exception;

public class AuthenticationException extends BusinessException {
    public AuthenticationException(String message) {
        super(message);
    }
}
