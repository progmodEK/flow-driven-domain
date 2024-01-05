package com.progmod.poc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PickItemRequest(@JsonProperty("skuId") String skuId, @JsonProperty("qty") int qty) {

}
