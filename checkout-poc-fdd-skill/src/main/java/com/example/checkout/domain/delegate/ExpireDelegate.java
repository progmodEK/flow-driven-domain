package com.example.checkout.domain.delegate;

import com.example.checkout.domain.Checkout;
import com.progmod.flow.domain.service.delegate.SystemActionDelegate;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * SYSTEM action: abandoned-cart timeout. Fires automatically via the transition timer if the
 * customer never advances past CART, moving the checkout to EXPIRED.
 */
@Component
@Log4j2
public class ExpireDelegate extends SystemActionDelegate<Checkout> {
  @Override
  public Checkout execute(final Checkout checkout, final Map<String, Object> ctx) {
    log.info("checkout {} expired (abandoned cart)", checkout.getId());
    return checkout;
  }
}
