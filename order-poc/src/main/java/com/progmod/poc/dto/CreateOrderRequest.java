package com.progmod.poc.dto;

import com.progmod.poc.domain.Item;
import java.util.List;
import lombok.Data;

@Data
public class CreateOrderRequest {
  List<Item> items;
}
