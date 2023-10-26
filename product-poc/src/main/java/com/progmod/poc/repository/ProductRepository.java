package com.progmod.poc.repository;

import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.poc.domain.Product;
import reactor.core.publisher.Flux;

public interface ProductRepository extends FlowRepository<com.progmod.poc.domain.Product, Long> {

  Flux<Product> findAll();
}
