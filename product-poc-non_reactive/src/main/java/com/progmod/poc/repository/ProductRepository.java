package com.progmod.poc.repository;

import com.progmod.flow.domain.port.FlowRepository;
import com.progmod.poc.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, FlowRepository<Product, String> {


}
