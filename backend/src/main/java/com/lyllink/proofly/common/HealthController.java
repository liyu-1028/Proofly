package com.lyllink.proofly.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final Environment environment;
    private final String applicationName;

    public HealthController(
            Environment environment,
            @Value("${spring.application.name:proofly-backend}") String applicationName
    ) {
        this.environment = environment;
        this.applicationName = applicationName;
    }

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse(
                applicationName,
                "UP",
                environment.getActiveProfiles()
        ));
    }
}
