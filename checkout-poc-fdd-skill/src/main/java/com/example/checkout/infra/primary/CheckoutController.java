package com.example.checkout.infra.primary;

import com.example.checkout.domain.Checkout;
import com.example.checkout.domain.LineItem;
import com.example.checkout.domain.flow.CheckoutAction;
import com.example.checkout.domain.flow.CheckoutFlowType;
import com.example.checkout.dto.CreateCheckoutRequest;
import com.example.checkout.dto.PaymentRequest;
import com.example.checkout.dto.ShippingRequest;
import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.flow.domain.service.FlowEngine;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
@RequestMapping("/checkouts")
@RequiredArgsConstructor
public class CheckoutController {
  protected final FlowRepository<Checkout, UUID> repository;
  protected final FlowEngine<Checkout, UUID> flowEngine;

  @PostMapping
  public ResponseEntity<Checkout> create(@RequestBody final CreateCheckoutRequest req) {
    final Checkout checkout = Checkout.builder()
        .cartRef(req.cartRef())
        .items(req.items().stream().map(i ->
            LineItem.builder()
                .skuId(i.skuId())
                .name(i.name())
                .qty(i.qty())
                .unitPriceCents(i.unitPriceCents())
                .build()).toList())
        .build();
    return ResponseEntity.ok(
        flowEngine.makeFlowable(checkout, CheckoutFlowType.DEFAULT, Map.of()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Checkout> get(@PathVariable final String id) {
    return ResponseEntity.of(repository.findById(UUID.fromString(id)));
  }

  @PostMapping("/{id}/start")
  public ResponseEntity<Checkout> start(@PathVariable final String id) {
    return ResponseEntity.ok(flowEngine.<Map<String, String>, Checkout>applyAction(
        UUID.fromString(id), CheckoutAction.START_CHECKOUT, Map.of()));
  }

  @PostMapping("/{id}/shipping")
  public ResponseEntity<Checkout> shipping(@PathVariable final String id,
                                           @RequestBody final ShippingRequest req) {
    return ResponseEntity.ok(flowEngine.<ShippingRequest, Checkout>applyAction(
        UUID.fromString(id), CheckoutAction.SET_SHIPPING, req));
  }

  @PostMapping("/{id}/pay")
  public ResponseEntity<Checkout> pay(@PathVariable final String id,
                                      @RequestBody final PaymentRequest req) {
    return ResponseEntity.ok(flowEngine.<PaymentRequest, Checkout>applyAction(
        UUID.fromString(id), CheckoutAction.SUBMIT_PAYMENT, req));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<Checkout> cancel(@PathVariable final String id) {
    return ResponseEntity.ok(flowEngine.<Map<String, String>, Checkout>applyAction(
        UUID.fromString(id), CheckoutAction.CANCEL, Map.of()));
  }
}
