package com.javarush.taskmanager.exception;

public class InvalidTokenException extends BusinessException {
    public InvalidTokenException(String message) {
        super(message);
    }
}