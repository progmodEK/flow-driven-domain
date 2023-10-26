package com.progmod.poc.repository;

import com.progmod.poc.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "flow")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductEntity implements Persistable<String> {

  @Id
  @Builder.Default
  private String id = UUID.randomUUID().toString();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "flow_data")
  private Product product;

  @Transient
  private boolean newEntity;

  public void setAsNew() {
    this.newEntity = true;
  }

  /**
   * This method is called by R2DBCRepository save() to check weather to do an insert or update.
   *
   * @return true if new, else false
   */
  @Override
  public boolean isNew() {
    return this.newEntity;
  }

}