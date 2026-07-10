package com.example.checkout.domain.delegate;

import com.example.checkout.domain.Checkout;
import com.example.checkout.dto.ShippingRequest;
import com.progmod.flow.domain.service.delegate.ActionDelegate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class SetShippingDelegate
    implements ActionDelegate<Checkout, ShippingRequest, Checkout> {
  @Override
  public Checkout execute(
      final Checkout checkout, final Map<String, Object> ctx, final ShippingRequest in) {
    checkout.applyShipping(in);
    log.info("shipping set for checkout {}", checkout.getId());
    return checkout;
  }
}
