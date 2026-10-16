package org.Proect.domian.Checkpoints.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChekcpointsException extends RuntimeException {
    public ChekcpointsException(String message) {
        super(message);
    }
}
