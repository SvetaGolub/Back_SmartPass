package org.Proect.domian.User.exception;

// Статус HTTP задаётся централизованно
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}