package org.Proect.domian.QR.mapper;

import org.Proect.domian.QR.dto.TokenResponse;
import org.Proect.domian.QR.model.QR;
import org.springframework.stereotype.Component;

@Component
public class QRMapper {

    public static TokenResponse toResponse(QR qr) {
        if (qr == null) return null;

        return new TokenResponse(qr.getToken(), qr.getExpiresAt());
    }
}
