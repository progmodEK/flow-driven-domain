package com.progmod.poc;

import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.flow.infra.database.BaseR2dbcJsonRepository;
import com.progmod.poc.domain.Order;
import com.progmod.poc.domain.flow.OrderFlowType;
import java.util.UUID;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PocConfig {

  @Bean
  public FlowRepository<Order, UUID> orderPostgresRepository() {
    return new BaseR2dbcJsonRepository<>(Order.class)
        .setTableInfo("flow_uuid", "flow_data");
  }

  @Bean
  FlowEngine<Order, UUID> flowEngine(final ApplicationContext applicaioncontext) {
    return new FlowEngine<>(UUID.class, OrderFlowType.class, orderPostgresRepository());
  }
}
