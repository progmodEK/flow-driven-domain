package com.progmod.poc.controller;

import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.flow.domain.service.parser.definition.FlowDefinition;
import com.progmod.poc.domain.Product;
import com.progmod.poc.domain.delegate.DelegateParams;
import com.progmod.poc.domain.flow.ProductAction;
import com.progmod.poc.domain.flow.ProductflowType;
import com.progmod.poc.dto.ApiResponse;
import com.progmod.poc.dto.CreateProductProcessRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Abstract Checkout rest controller with endpoints.
 * <p>POST /checkout to create a new checkout</p>
 */
@RestController
@Log4j2
@RequestMapping("/pocs")
@RequiredArgsConstructor
public class PocController {

  protected final FlowRepository<Product, Long> pocRepository;
  protected final FlowEngine<Product, Long> flowEngine;

  @PostMapping
  public Mono<ResponseEntity<Product>> createPoc(
      @RequestBody final CreateProductProcessRequest createProductProcessRequest) {
//  @PostMapping
//  public Mono<ResponseEntity<Product>> createPoc() {
//    final Product productProcess = Product.builder().product(
//        Product.builder().id(createProductProcessRequest.getProductId()).build()
//    ).build();

    return flowEngine.makeFlowable(Product.builder().id(createProductProcessRequest.getProductId()).build(),
            ProductflowType.DEFAULT, Map.of())
        .map(ResponseEntity::ok);

//    return pocRepository.create(Product.builder().id(createProductProcessRequest.getProductId()).build())
//        .map(ResponseEntity::ok);

//    return flowEngine.init(Product.builder().build(), ProductflowType.DEFAULT, Map.of())
//        .flatMap(
//            product -> flowEngine.<DelegateParams, ApiResponse>applyAction(product, ProductAction.CREATE_REFERENCE_API,
//                    DelegateParams.of())
//                .flatMap(
//                    apiresponse -> flowEngine.<DelegateParams, Product>applyAction(product, ProductAction.SET_REFERENCE,
//                        DelegateParams.of(apiresponse.getReferenceId())))
//        )
//        .map(ResponseEntity::ok);
  }


  @GetMapping("/")
  public Flux<Product> getAllProductProcess() {
    return pocRepository.findAll();
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<Product>> getProductProcess(@PathVariable final String id) {
    return pocRepository.findById(Long.parseLong(id))
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @GetMapping("/{id}/flow-definition")
  public Mono<ResponseEntity<FlowDefinition>> getFlow(@PathVariable final String id) {
    return flowEngine.getFlowDefinition(Long.parseLong(id)).map(ResponseEntity::ok);
  }


  @PostMapping("/{id}/create_external_reference")
  public Mono<ResponseEntity<ApiResponse>> createReference(@PathVariable final String id) {
    return flowEngine.<DelegateParams, ApiResponse>applyAction(Long.parseLong(id),
            ProductAction.CREATE_REFERENCE_API,
            DelegateParams.of())
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @PostMapping("/{id}/set_reference")
  public Mono<ResponseEntity<Product>> setReference(@PathVariable final String id,
                                                    @RequestBody final Map<String, String> body) {
    return flowEngine.<DelegateParams, Product>applyAction(Long.parseLong(id), ProductAction.SET_REFERENCE,
            DelegateParams.of(body.get("referenceId")))
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

}
