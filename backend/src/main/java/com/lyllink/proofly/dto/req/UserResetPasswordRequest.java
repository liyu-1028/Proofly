package com.lyllink.proofly.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserResetPasswordRequest(
        @NotBlank
        @Size(min = 6, max = 72)
        String password
) {
}
