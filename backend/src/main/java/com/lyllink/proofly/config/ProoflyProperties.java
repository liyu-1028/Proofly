package com.lyllink.proofly.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "proofly")
public class ProoflyProperties {
    private String reviewBaseUrl = "http://localhost:5173/review/";
    private Billing billing = new Billing();

    @Getter
    @Setter
    public static class Billing {
        private BigDecimal basePricePerMonth = new BigDecimal("29.00");
        private Map<Integer, BigDecimal> discounts;
        private Xpay xpay = new Xpay();

        @Getter
        @Setter
        public static class Xpay {
            private String appId = "mock_app_id";
            private String appKey = "mock_app_key";
            private String apiUrl = "http://localhost:8080/api/public/webhook/xpay";
        }
    }
}
