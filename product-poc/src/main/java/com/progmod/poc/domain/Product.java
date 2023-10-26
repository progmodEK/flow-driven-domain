package com.progmod.poc.domain;

import com.progmod.flow.domain.model.Flow;
import com.progmod.flow.domain.model.Flowable;
import java.util.concurrent.ThreadLocalRandom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Flowable<Long> {
  @Builder.Default
  private Long id = ThreadLocalRandom.current().nextLong();
  private String reference;

  private String state;
  private Flow flow;
}
