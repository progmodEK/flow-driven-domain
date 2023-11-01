package com.progmod.poc.domain;

import com.progmod.poc.repository.CustomJacksonJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "flow")
public class Product implements Flowable {
  @Id
  @Builder.Default
  private final String id = UUID.randomUUID().toString();
  @Column(name = "reference")
  private String reference;
//  private List<String> tempState;

  @Column(name = "state")
  private String state;
  //  @JdbcTypeCode(SqlTypes.JSON)
  @Convert(converter = CustomJacksonJsonConverter.class)
  @Column(name = "flow_data_string")
//  @Column(name = "flow_data")
  private Flow flow;
}
