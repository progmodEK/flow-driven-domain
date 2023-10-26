package com.progmod.poc.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;

public class CustomJacksonJsonFormatMapper implements FormatMapper {

  //  private final FormatMapper delegate;
  private final FormatMapper delegate;

  public CustomJacksonJsonFormatMapper() {
    final ObjectMapper objectMapper = createObjectMapper();
    delegate = new JacksonJsonFormatMapper(objectMapper);
  }

  private static ObjectMapper createObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    return objectMapper;
  }

  @Override
  public <T> T fromString(final CharSequence charSequence, final JavaType<T> javaType,
      final WrapperOptions wrapperOptions) {
    return delegate.fromString(charSequence, javaType, wrapperOptions);
  }

  @Override
  public <T> String toString(final T t, final JavaType<T> javaType, final WrapperOptions wrapperOptions) {
    return delegate.toString(t, javaType, wrapperOptions);
  }


}