package org.Proect.config;

import org.Proect.domian.User.exception.RoleNotFoundException;
import org.Proect.domian.User.exception.UserAlreadyExistsException;
import org.Proect.domian.User.exception.UserNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    // Вспомогательный метод для автоматического добавления времени ко всем ошибкам
    private ProblemDetail createProblemDetail(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("timestamp", Instant.now()); // Перенесли фичу из 2-го файла
        return pd;
    }

    @ExceptionHandler({UserNotFoundException.class, RoleNotFoundException.class})
    public ProblemDetail notFound(RuntimeException e) {
        ProblemDetail pd = createProblemDetail(HttpStatus.NOT_FOUND, e.getMessage());
        pd.setTitle("Ресурс не найден");
        return pd;
    }

    @ExceptionHandler({UserAlreadyExistsException.class, IllegalStateException.class})
    public ProblemDetail conflict(RuntimeException e) {
        ProblemDetail pd = createProblemDetail(HttpStatus.CONFLICT, e.getMessage());
        pd.setTitle("Ошибка бизнес-логики");
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integrity(DataIntegrityViolationException e) {
        ProblemDetail pd = createProblemDetail(HttpStatus.CONFLICT, "Логин, email или телефон уже используются");
        pd.setTitle("Конфликт данных");
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        ProblemDetail pd = createProblemDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setTitle("Неверный запрос");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));

        ProblemDetail pd = createProblemDetail(HttpStatus.BAD_REQUEST, "Ошибка валидации");
        pd.setTitle("Ошибка валидации");
        pd.setProperty("errors", errors);
        return pd;
    }
}