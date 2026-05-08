package com.lyllink.proofly.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserCreateRequest(
        @NotBlank
        @Size(max = 64)
        String username,

        @NotBlank
        @Size(max = 64)
        String nickname,

        @Size(max = 30)
        String phone,

        @Size(max = 128)
        String email,

        @NotBlank
        @Size(min = 6, max = 72)
        String password,

        List<String> roleCodes
) {
}
