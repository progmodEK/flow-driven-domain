package com.progmod.poc;

import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.poc.domain.Product;
import com.progmod.poc.domain.flow.ProductFlowType;
import com.progmod.poc.repository.ProductRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PocConfig {

//  @Bean
//  public FlowRepository<Product> productProcessPostGresRepository() {
//    return new BasePostGresJsonRepository<>(Product.class)
//        .setTableInfo("flow", BasePostGresJsonRepository.IdType.STRING, "flow_data");
//  }

  @Bean
  FlowEngine<Product, String> flowEngine(final ProductRepository productRepository) {
    return new FlowEngine<>(String.class, ProductFlowType.class, productRepository);
  }
}
