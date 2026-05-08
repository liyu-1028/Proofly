package com.lyllink.proofly.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreUpdateRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 50)
        String contactName,

        @Size(max = 30)
        String contactPhone
) {
}
