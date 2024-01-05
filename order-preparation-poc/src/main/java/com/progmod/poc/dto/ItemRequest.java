package com.progmod.poc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ItemRequest(@JsonProperty("skuId") String skuId, @JsonProperty("name") String name,
                          @JsonProperty("qty") int qty) {

}
