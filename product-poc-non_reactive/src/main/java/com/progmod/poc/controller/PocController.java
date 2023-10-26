package com.progmod.poc.controller;

import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.poc.domain.Product;
import com.progmod.poc.domain.delegate.DelegateParams;
import com.progmod.poc.domain.flow.ProductAction;
import com.progmod.poc.domain.flow.ProductFlowType;
import com.progmod.poc.dto.ApiResponse;
import com.progmod.poc.dto.CreateProductProcessRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Abstract Checkout rest controller with endpoints.
 * <p>POST /checkout to create a new checkout</p>
 */
@Controller
@Log4j2
@RequestMapping("/pocs")
@RequiredArgsConstructor
public class PocController {

  protected final CrudRepository<Product, String> pocRepository;
  protected final FlowEngine<Product, String> flowEngine;

  @PostMapping
  public ResponseEntity<Product> createPoc(
      @RequestBody final CreateProductProcessRequest createProductProcessRequest) {
//  @PostMapping
//  public Mono<ResponseEntity<Product>> createPoc() {
//    final Product productProcess = Product.builder().product(
//        Product.builder().id(createProductProcessRequest.getProductId()).build()
//    ).build();

    final Product product =
        flowEngine.makeFlowable(Product.builder().id(createProductProcessRequest.getProductId()).build(),
            ProductFlowType.DEFAULT, Map.of());
    return ResponseEntity.ok(product);
  }


  @GetMapping("/{id}")
  public ResponseEntity<Product> getProductProcess(@PathVariable final String id) {
    return ResponseEntity.of(pocRepository.findById(id));
  }


  @PostMapping("/{id}/create_external_reference")
  public ResponseEntity<ApiResponse> createReference(@PathVariable final String id) {
    return ResponseEntity.ok(
        flowEngine.applyAction(id, ProductAction.CREATE_REFERENCE_API,
            DelegateParams.of()));
//    final Product product = pocRepository.findById(id);
//    product.getFlow().setState("ELIE");
//    product.setReference("123");
//    pocRepository.update(product);
//    return ResponseEntity.ok(ApiResponse.builder().build());
  }

  @PostMapping("/{id}/set_reference")
//  @Transactional(noRollbackFor = RuntimeException.class)
  public ResponseEntity<Product> setReference(@PathVariable final String id,
                                              @RequestBody final Map<String, String> body) {
    return ResponseEntity.ok(flowEngine.applyAction(id, ProductAction.SET_REFERENCE,
        DelegateParams.of(body.get("referenceId"))));
  }

}
