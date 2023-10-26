package com.progmod.poc.repository;

import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.poc.domain.Order;
import java.util.UUID;
import reactor.core.publisher.Flux;

public interface OrderRepository extends FlowRepository<Order, UUID> {

  @Override
  Flux<Order> findAll();
}
