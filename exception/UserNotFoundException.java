package com.cvbuilder.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public UserNotFoundException(Long userId) {
        super("Kullanıcı bulunamadı: " + userId);
    }
}