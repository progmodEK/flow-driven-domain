package com.progmod.poc;

import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.flow.infra.postgres.BasePostgresJsonRepository;
import com.progmod.poc.domain.OrderPreparation;
import com.progmod.poc.domain.flow.OrderPreparationFlowType;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PocConfig {

  @Bean
  public FlowRepository<OrderPreparation, UUID> orderPostgresRepository() {
    return new BasePostgresJsonRepository<>(OrderPreparation.class)
        .setTableInfo("order_preparation", "data");
  }

  @Bean
  FlowEngine<OrderPreparation, UUID> flowEngine(final FlowRepository orderPostgresRepository) {
    return new FlowEngine<>(UUID.class, OrderPreparationFlowType.class, orderPostgresRepository);
  }
}
