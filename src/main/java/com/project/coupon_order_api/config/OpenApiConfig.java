package com.project.coupon_order_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI couponOrderApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Coupon Order API")
                        .description("Product and Coupon management with coupon-based order pricing")
                        .version("v1"));
    }
}
