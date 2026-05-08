package com.lyllink.proofly.auth;

import java.util.List;

public record MeResponse(
        Long userId,
        Long storeId,
        String username,
        String nickname,
        String phone,
        String status,
        List<String> roles
) {
}
