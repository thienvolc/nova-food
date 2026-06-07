package com.nova.food.app.dto.response;

import jakarta.annotation.Nullable;

public record ResponseDto(
        Meta meta,
        @Nullable Object data
) {
}
