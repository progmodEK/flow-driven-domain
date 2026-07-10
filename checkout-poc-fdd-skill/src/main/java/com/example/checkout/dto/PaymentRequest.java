package com.example.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentRequest(
    @JsonProperty("paymentRef") String paymentRef,
    @JsonProperty("amountCents") long amountCents) {}
