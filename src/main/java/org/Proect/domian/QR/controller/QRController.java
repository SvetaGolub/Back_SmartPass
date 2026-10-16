package org.Proect.domian.QR.controller;

import lombok.RequiredArgsConstructor;
import org.Proect.domian.QR.dto.TokenResponse;
import org.Proect.domian.QR.service.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qr")
@RequiredArgsConstructor
public class QRController {

    private final TokenService tokenService;

    //Сгенерировать или обновить динамический QR-токен для сотрудника.
    @PostMapping("/generate/{userId}")
    @ResponseStatus(HttpStatus.CREATED) // Возвращает статус 201 Created при успешном создании
    public TokenResponse generateToken(@PathVariable Integer userId) {
        return tokenService.generateTokenForUser(userId);
    }

    //Получить информацию о текущем активном токене пользователя.
    @GetMapping("/status/{userId}")
    public TokenResponse getTokenStatus(@PathVariable Integer userId) {
        return tokenService.getTokenStatus(userId);
    }
}
