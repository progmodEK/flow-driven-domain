package com.progmod.poc;

import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.flow.infra.database.BaseR2dbcPostgresJsonRepository;
import com.progmod.poc.domain.OrderPreparation;
import com.progmod.poc.domain.flow.OrderPreparationFlowType;
import java.util.UUID;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PocConfig {

  @Bean
  public FlowRepository<OrderPreparation, UUID> orderPostgresRepository() {
    return new BaseR2dbcPostgresJsonRepository<>(OrderPreparation.class)
        .setTableInfo("order_preparation", "data");
  }

  @Bean
  FlowEngine<OrderPreparation, UUID> flowEngine(final ApplicationContext applicaioncontext) {
    return new FlowEngine<>(UUID.class, OrderPreparationFlowType.class, orderPostgresRepository());
  }
}
