package org.example.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.ExternalDocumentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI openAPI() {
        Server localServer = new Server()
                .url("/")
                .description("Local Development Server");

        Server productionServer = new Server()
                .url("https://api.sentio-systems.com")
                .description("Production Server");

        Contact contact = new Contact()
                .name("Sentio Systems Team")
                .email("support@sentio-systems.com")
                .url("https://github.com/lucawalz/sentio-systems");

        License license = new License()
                .name("MIT License")
                .url("https://github.com/lucawalz/sentio-systems/blob/main/LICENSE");

        Info info = new Info()
                .title("Sentio Systems API")
                .version("1.0.0")
                .description("""
                    # Sentio Environmental Monitoring Platform API
                    
                    Comprehensive API for environmental monitoring with IoT sensors and AI-powered wildlife detection.
                    
                    ## 🌟 Features
                    - **🌤️ Weather Data**: Real-time sensor data from IoT devices
                    - **📊 Weather Forecasts**: 7-day forecasts from Open-Meteo API
                    - **🕰️ Historical Weather**: Historical weather data archive
                    - **⚠️ Weather Alerts**: Real-time warnings from BrightSky API
                    - **🐦 Animal Detection**: AI-powered wildlife monitoring
                    - **📍 Location Services**: IP-based geolocation
                    - **🤖 AI Summaries**: AI-generated environmental insights
                    
                    ## 🏗️ Architecture
                    
                    ### Synchronous Endpoints (GET)
                    - Return cached data **immediately** from PostgreSQL
                    - Average response time: **< 100ms**
                    - Use for: Dashboard queries, analytics, reports
                    
                    ### Asynchronous Endpoints (POST)
                    - Accept data and return **immediately** (201/202)
                    - Background processing via AI services
                    - Results available via GET after processing
                    - Use for: Data submission, AI classification
                    
                    ### MQTT Integration (IoT Devices)
                    Preferred method for IoT devices: 
                    - **Topics**: `weather`, `camera`, `birds/detected`
                    - Lower latency, better for unreliable networks
                    - Automatic reconnection and QoS
                    
                    ## 🔐 Authentication
                    **Current**:  All endpoints are public
                    **Future**: OAuth 2.0 / JWT in v2.0
                    
                    ## 📊 Rate Limits
                    - **Development**: No limits
                    - **Production**: 1000 requests/hour per IP
                    """)
                .contact(contact)
                .license(license);

        ExternalDocumentation externalDocs = new ExternalDocumentation()
                .description("📚 Complete API Documentation")
                .url("https://github.com/lucawalz/sentio-systems/blob/main/sentio-backend/docs/api-documentation.md");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, productionServer))
                .externalDocs(externalDocs);
    }
}