package com.library.api.config;

import com.library.api.dto.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI libraryOpenApi() {
        Components components = new Components()
                        .addResponses("BadRequest", error("Invalid request or field validation failure"))
                        .addResponses("NotFound", error("Requested library record does not exist"))
                        .addResponses("Conflict", error("Operation conflicts with the current library state"))
                        .addResponses("InternalError", error("Unexpected server error"));
        ModelConverters.getInstance().read(ErrorResponse.class).forEach(components::addSchemas);
        return new OpenAPI().info(new Info().title("Library Management API").version("lab-4")
                .description("Catalogue, members, borrowing and reservations"))
                .components(components);
    }

    private ApiResponse error(String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))));
    }
}
