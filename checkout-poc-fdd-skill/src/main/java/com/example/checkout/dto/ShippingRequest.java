package com.example.checkout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ShippingRequest(@JsonProperty("address") String address) {}
