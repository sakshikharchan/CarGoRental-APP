package com.example.demo.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    // ✅ ROOT FIX: Without JavaTimeModule registered, Jackson cannot
    //    deserialize LocalDate from JSON at all — the field stays null
    //    no matter what @JsonFormat you put on it.
    //    This registers the module globally for ALL controllers.
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // Define the exact format frontend sends: "2026-02-21"
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(dateFormatter));
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(dateFormatter));

        return new ObjectMapper()
                .registerModule(javaTimeModule)
                // Don't serialize dates as timestamps [1234567890]
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
