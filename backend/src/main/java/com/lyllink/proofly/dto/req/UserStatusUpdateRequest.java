package com.lyllink.proofly.dto.req;

import jakarta.validation.constraints.NotBlank;

public record UserStatusUpdateRequest(
        @NotBlank
        String status
) {
}
