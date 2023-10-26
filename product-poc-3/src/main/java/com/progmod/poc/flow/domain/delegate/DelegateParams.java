package com.progmod.poc.flow.domain.delegate;

import lombok.Builder;

@Builder
public class DelegateParams {

  public String referenceId;

  public static DelegateParams of() {
    return DelegateParams.builder().build();
  }

  public static DelegateParams of(final String referenceId) {
    return DelegateParams.builder().referenceId(referenceId).build();
  }

}
