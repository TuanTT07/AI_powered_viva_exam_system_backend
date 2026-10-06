package cocxanhcoder.viva.exam.system.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Swagger: thêm nút "Authorize" để dán JWT.
 * Cách dùng: gọi POST /api/auth/login -> copy accessToken -> bấm Authorize -> dán token (không cần chữ "Bearer").
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info().title("AIVES API").version("v1")
                        .description("AI-powered Viva Exam System - Backend API"))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Áp dụng token cho mọi API trong Swagger
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
