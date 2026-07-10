package com.example.checkout;

import com.example.checkout.domain.Checkout;
import com.example.checkout.domain.flow.CheckoutFlowType;
import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.flow.infra.postgres.BasePostgresJsonRepository;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CheckoutConfig {

  @Bean
  public FlowRepository<Checkout, UUID> checkoutPostgresRepository() {
    return new BasePostgresJsonRepository<>(Checkout.class)
        .setTableInfo("checkout", "data");
  }

  @Bean
  FlowEngine<Checkout, UUID> flowEngine(final FlowRepository checkoutPostgresRepository) {
    return new FlowEngine<>(UUID.class, CheckoutFlowType.class, checkoutPostgresRepository);
  }
}
