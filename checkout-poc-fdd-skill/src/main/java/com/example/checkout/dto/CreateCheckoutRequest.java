package com.example.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CreateCheckoutRequest(
    @JsonProperty("cartRef") String cartRef,
    @JsonProperty("items") List<LineItemRequest> items) {}
