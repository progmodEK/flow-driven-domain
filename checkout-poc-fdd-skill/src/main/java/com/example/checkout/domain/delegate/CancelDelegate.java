package com.example.checkout.domain.delegate;

import com.example.checkout.domain.Checkout;
import com.progmod.flow.domain.service.delegate.ActionDelegate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class CancelDelegate
    implements ActionDelegate<Checkout, Map<String, String>, Checkout> {
  @Override
  public Checkout execute(
      final Checkout checkout, final Map<String, Object> ctx, final Map<String, String> in) {
    log.info("checkout {} cancelled by customer", checkout.getId());
    return checkout;
  }
}
