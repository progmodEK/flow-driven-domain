package com.example.checkout.domain.delegate;

import com.example.checkout.domain.Checkout;
import com.progmod.flow.domain.service.DelegateException;
import com.progmod.flow.domain.service.delegate.SystemActionDelegate;
import java.util.Map;
import java.util.Random;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * SYSTEM action: calls the payment provider to confirm the authorization. Simulates a transient
 * gateway failure by throwing a {@link DelegateException} whose code (1 == JSON key "001") drives
 * the retry / RETRY_PAYMENT transition; after the configured retries it lands in PAYMENT_FAILED.
 */
@Component
@Log4j2
public class ConfirmPaymentDelegate extends SystemActionDelegate<Checkout> {
  @Override
  public Checkout execute(final Checkout checkout, final Map<String, Object> ctx) {
    log.info("confirming payment {} for checkout {}", checkout.getPaymentRef(), checkout.getId());
    if (new Random().nextBoolean()) { // simulate a transient gateway failure
      throw new DelegateException(1, "payment gateway temporarily unavailable");
    }
    log.info("payment confirmed for checkout {}", checkout.getId());
    return checkout;
  }
}
