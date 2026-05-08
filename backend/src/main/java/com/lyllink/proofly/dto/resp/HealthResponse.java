package com.lyllink.proofly.dto.resp;

public record HealthResponse(
        String application,
        String status,
        String[] profiles
) {
}
