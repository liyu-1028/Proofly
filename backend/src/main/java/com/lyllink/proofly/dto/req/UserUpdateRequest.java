package com.lyllink.proofly.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserUpdateRequest(
        @NotBlank
        @Size(max = 64)
        String nickname,

        @Size(max = 30)
        String phone,

        @Size(max = 128)
        String email,

        List<String> roleCodes
) {
}
