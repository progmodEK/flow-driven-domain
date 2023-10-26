package com.progmod.poc;

import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.flow.infra.database.BaseR2dbcJsonRepository;
import com.progmod.poc.domain.Product;
import com.progmod.poc.domain.flow.ProductflowType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PocConfig {

  @Bean
  public FlowRepository<Product, Long> productProcessPostGresRepository() {
    return new BaseR2dbcJsonRepository<>(Product.class)
        .setTableInfo("flow_long", "flow_data");
  }

  @Bean
  FlowEngine<Product, Long> flowEngine() {
    return new FlowEngine<>(Long.class, ProductflowType.class, productProcessPostGresRepository());
  }
}
