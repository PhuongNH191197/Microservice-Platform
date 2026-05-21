package com.platform.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**").uri("lb://auth-service"))
                .route("file-service", r -> r.path("/api/files/**").uri("lb://file-service"))
                .route("payment-gateway-service", r -> r.path("/api/payments/**").uri("lb://payment-gateway-service"))
                .route("credit-wallet-service", r -> r.path("/api/wallet/**").uri("lb://credit-wallet-service"))
                .route("crbt-campaign-service", r -> r.path("/api/campaigns/**").uri("lb://crbt-campaign-service"))
                .route("crbt-community-library", r -> r.path("/api/library/**").uri("lb://crbt-community-library"))
                .route("audio-generation-service", r -> r.path("/api/audio/**").uri("lb://audio-generation-service"))
                .route("crbt-credit-transaction-service", r -> r.path("/api/credits/**").uri("lb://crbt-credit-transaction-service"))
                .route("crbt-core-adapter", r -> r.path("/api/core-adapter/**").uri("lb://crbt-core-adapter"))
                .build();
    }
}
