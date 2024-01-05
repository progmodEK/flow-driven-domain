package com.progmod.poc.domain;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.progmod.flow.domain.model.Flow;
import com.progmod.flow.domain.model.Flowable;
import com.progmod.poc.dto.PickItemRequest;
import com.progmod.poc.dto.PickItemsRequest;
import java.beans.Transient;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"id", "orderRef", "items", "state", "flow"})
public class OrderPreparation implements Flowable<UUID> {
  @Builder.Default
  private UUID id = UUID.randomUUID();
  private String orderRef;
  private List<Item> items;

  private String state;
  private Flow flow;


  /**
   * Update Preparation and check Aggregate invariants.
   */
  public void updatePreparation(final PickItemsRequest pickItemsRequest) {
    // check picked items exist
    final List<String> pickedSkus = pickItemsRequest.pickItems().stream()
        .map(PickItemRequest::skuId)
        .toList();
    final List<String> existingSkus = items.stream()
        .map(Item::getSkuId)
        .toList();
    final boolean allExists = pickedSkus.stream().allMatch(s -> existingSkus.contains(s));
    if (!allExists) {
      throw new IllegalArgumentException("prepared items does not exists");
    }

    // update Items preparation
    pickItemsRequest.pickItems().stream()
        .forEach(
            pickItemRequest -> items.stream().filter(item -> item.skuId.equals(pickItemRequest.skuId())).findFirst()
                .map(item -> item.updatePreparedQty(pickItemRequest.qty()))
        );
  }

  @Transient
  public boolean isFullyPrepared() {
    return items.stream()
        .filter(item -> item.qty == item.qtyPrepared)
        .toList().size() == items.size();

  }
}
