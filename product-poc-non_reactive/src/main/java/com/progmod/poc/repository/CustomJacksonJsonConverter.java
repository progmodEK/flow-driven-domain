package com.progmod.poc.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.progmod.flow.domain.model.Flow;
import jakarta.persistence.AttributeConverter;

public class CustomJacksonJsonConverter implements AttributeConverter<Flow, String> {

  //  private final FormatMapper delegate;
  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  @Override
  public String convertToDatabaseColumn(final Flow flow) {
    try {
      return objectMapper.writeValueAsString(flow);
    } catch (final JsonProcessingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public Flow convertToEntityAttribute(final String dbData) {
    try {
      return objectMapper.readValue(dbData, Flow.class);
    } catch (final JsonProcessingException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}