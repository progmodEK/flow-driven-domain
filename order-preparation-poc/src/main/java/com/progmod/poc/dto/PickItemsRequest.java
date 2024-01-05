package com.progmod.poc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PickItemsRequest(@JsonProperty("pickItems") List<PickItemRequest> pickItems) {
}
