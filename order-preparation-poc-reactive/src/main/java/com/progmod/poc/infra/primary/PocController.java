package com.progmod.poc.infra.primary;

import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import com.progmod.poc.domain.Item;
import com.progmod.poc.domain.OrderPreparation;
import com.progmod.poc.domain.flow.OrderPreparationAction;
import com.progmod.poc.domain.flow.OrderPreparationFlowType;
import com.progmod.poc.dto.CreateOrderPreparationRequest;
import com.progmod.poc.dto.PickItemsRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Log4j2
@RequestMapping("/pocs")
@RequiredArgsConstructor
public class PocController {

  protected final FlowRepository<OrderPreparation, UUID> orderPreparationRepository;
  protected final FlowEngine<OrderPreparation, UUID> flowEngine;
  private ApplicationContext applicaioncontext;

  @PostMapping
  public Mono<ResponseEntity<OrderPreparation>> createOrderPreparation(
      @RequestBody final CreateOrderPreparationRequest createProductProcessRequest) {

    final OrderPreparation orderPreparation = OrderPreparation.builder()
        .orderRef(createProductProcessRequest.orderRef())
        .items(createProductProcessRequest.items().stream()
            .map(
                itemRequest -> Item.builder().skuId(itemRequest.skuId()).name(itemRequest.name()).qty(itemRequest.qty())
                    .build()
            ).toList())
        .build();
    return flowEngine.makeFlowable(orderPreparation, OrderPreparationFlowType.DEFAULT, Map.of())
        .map(ResponseEntity::ok);
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<OrderPreparation>> getOrderPreparation(@PathVariable final String id) {
    return orderPreparationRepository.findById(UUID.fromString(id)).map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @PostMapping("/{id}/start-preparation")
  public Mono<ResponseEntity<OrderPreparation>> startPreparation(@PathVariable final String id) {
    return flowEngine.<Map, OrderPreparation>applyAction(UUID.fromString(id),
            OrderPreparationAction.START_PREPARATION,
            Map.of())
        .map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @PostMapping("/{id}/pick-items")
  public Mono<ResponseEntity<OrderPreparation>> pickItems(@PathVariable final String id,
                                                          @RequestBody final PickItemsRequest pickItemsRequest) {
    return flowEngine.<PickItemsRequest, OrderPreparation>applyAction(UUID.fromString(id),
            OrderPreparationAction.PICK_ITEMS,
            pickItemsRequest)
        .map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @PostMapping("/{id}/pickup")
  public Mono<ResponseEntity<OrderPreparation>> pickup(@PathVariable final String id) {
    return flowEngine.<Map, OrderPreparation>applyAction(UUID.fromString(id),
            OrderPreparationAction.PICKUP,
            Map.of()).map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

}
