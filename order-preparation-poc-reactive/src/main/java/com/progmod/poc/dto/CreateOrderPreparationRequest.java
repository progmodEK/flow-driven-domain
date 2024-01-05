package com.progmod.poc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CreateOrderPreparationRequest(@JsonProperty("orderRef") String orderRef,
                                            @JsonProperty("items") List<ItemRequest> items) {
}
