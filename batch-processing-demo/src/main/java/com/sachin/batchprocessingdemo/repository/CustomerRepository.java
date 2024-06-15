package com.sachin.batchprocessingdemo.repository;

import com.sachin.batchprocessingdemo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
}
