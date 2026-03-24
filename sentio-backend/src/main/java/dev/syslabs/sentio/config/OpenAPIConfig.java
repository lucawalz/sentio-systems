package dev.syslabs.sentio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

        @Bean
        public OpenAPI openAPI() {
                Server localServer = new Server()
                                .url("/")
                                .description("Default Server");

                Contact contact = new Contact()
                                .name("API Support")
                                .email("support@example.org");

                License license = new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0");

                Info info = new Info()
                                .title("Sentio API Documentation")
                                .version("1.0.0")
                                .description("API documentation for Sentio application")
                                .contact(contact)
                                .license(license);

                SecurityScheme securityScheme = new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization");

                SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

                return new OpenAPI()
                                .info(info)
                                .servers(List.of(localServer))
                                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                                .addSecurityItem(securityRequirement);
        }
}