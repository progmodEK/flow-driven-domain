package com.progmod.poc.product.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse {
  String referenceId;
  String description;
}
