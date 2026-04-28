package com.lyllink.proofly.common;

public record HealthResponse(
        String application,
        String status,
        String[] profiles
) {
}
