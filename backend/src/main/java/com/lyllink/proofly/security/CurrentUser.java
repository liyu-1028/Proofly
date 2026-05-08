package com.lyllink.proofly.security;

import java.util.List;

public record CurrentUser(
        Long userId,
        Long storeId,
        String username,
        String nickname,
        List<String> roles,
        String tokenId
) {
}
