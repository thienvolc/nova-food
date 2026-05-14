package com.techora.app.dto.response;

import jakarta.annotation.Nullable;

public record ResponseDto(
        Meta meta,
        @Nullable Object data
) {
}
