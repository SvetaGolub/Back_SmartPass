package org.Proect.domian.User.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserCreateRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void rejectsMalformedEmailAddress() {
        UserCreateRequest request = validRequest();
        request.setEmail("not-an-email");

        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(violation ->
                violation.getPropertyPath().toString().equals("email")));
    }

    @Test
    void acceptsValidUserDetails() {
        assertTrue(validator.validate(validRequest()).isEmpty());
    }

    private static UserCreateRequest validRequest() {
        UserCreateRequest request = new UserCreateRequest();
        request.setRoleId(1);
        request.setZoneId(1);
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setNumberPhone("+70000000000");
        request.setEmail("ivanov@example.com");
        request.setPosition("Инженер");
        request.setAddress("Тестовый адрес");
        request.setBirthday(LocalDateTime.now().minusYears(30));
        request.setStage(LocalDateTime.now().minusYears(1));
        request.setLogin("ivanov");
        request.setPassword("test-password");
        return request;
    }
}
