package hu.upscale.akka.demo.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author László Zoltán
 */
public final class ObjectMapperProvider {

    private ObjectMapperProvider() {
        // Static class
    }

    public static ObjectMapper getObjectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return new ObjectMapper().registerModule(javaTimeModule)
            .disable(
                DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            )
            .disable(
                SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS,
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
            )
            .enable(
                DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS
            );
    }
}
