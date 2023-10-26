package com.progmod.poc.product.repository;

import com.progmod.poc.product.domain.Product;
import java.util.HashMap;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Here you define the Product Repository as a separate repository from the Flow,
 * cause its a totalyy different domain
 */

@Repository
public class ProductRepository {

  HashMap<String, Product> map = new HashMap<>() {{
    put("123", Product.builder().id("123").build());
  }};

  public Mono<Product> getProductById(final String productId) {
    return Mono.just(map.get(productId));
  }


  public Mono<Product> save(final Product product) {
    map.put(product.getId(), product);
    return Mono.just(product);

  }

}
