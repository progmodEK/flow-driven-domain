package com.example.checkout.domain.delegate;

import com.example.checkout.domain.Checkout;
import com.example.checkout.dto.PaymentRequest;
import com.progmod.flow.domain.service.delegate.ActionDelegate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class SubmitPaymentDelegate
    implements ActionDelegate<Checkout, PaymentRequest, Checkout> {
  @Override
  public Checkout execute(
      final Checkout checkout, final Map<String, Object> ctx, final PaymentRequest in) {
    checkout.applyPayment(in);
    log.info("payment {} submitted for checkout {} — awaiting async confirmation",
        checkout.getPaymentRef(), checkout.getId());
    return checkout;
  }
}
