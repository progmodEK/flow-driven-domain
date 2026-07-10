package com.example.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LineItemRequest(
    @JsonProperty("skuId") String skuId,
    @JsonProperty("name") String name,
    @JsonProperty("qty") int qty,
    @JsonProperty("unitPriceCents") long unitPriceCents) {}
