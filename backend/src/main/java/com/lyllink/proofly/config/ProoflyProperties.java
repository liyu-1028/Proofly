package com.lyllink.proofly.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "proofly")
public class ProoflyProperties {
    private String deploymentMode = "single-store";
    private Long defaultStoreId;
    private String reviewBaseUrl = "http://localhost:5173/review/";
}
