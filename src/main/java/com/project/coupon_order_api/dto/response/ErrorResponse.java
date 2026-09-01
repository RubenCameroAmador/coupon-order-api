package com.project.coupon_order_api.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String errorCode,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
