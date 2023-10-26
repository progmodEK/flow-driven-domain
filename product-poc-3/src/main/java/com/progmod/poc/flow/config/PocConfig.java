package com.progmod.poc.flow.config;

import com.progmod.flow.domain.model.BaseFlow;
import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.flow.infra.database.BaseR2dbcJsonRepository;
import com.progmod.poc.flow.domain.ProductFlowType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PocConfig {

  @Bean
  public FlowRepository<BaseFlow, String> flowPostGresRepository() {
    return new BaseR2dbcJsonRepository<>(BaseFlow.class)
        .setTableInfo("flow", "flow_data");
  }

  @Bean
  FlowEngine<BaseFlow, String> flowEngine() {
    return new FlowEngine<>(String.class, ProductFlowType.class, flowPostGresRepository());
  }
}
