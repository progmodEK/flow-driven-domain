package com.progmod.poc.domain;

import com.progmod.flow.domain.model.Flow;
import com.progmod.flow.domain.model.Flowable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Flowable<UUID> {
  @Builder.Default
  private UUID id = UUID.randomUUID();
  private List<Item> items;

  private String state;
  private Flow flow;
}
