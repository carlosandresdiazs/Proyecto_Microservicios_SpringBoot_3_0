package edu.unibague.products.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name="products")

public class Product {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
 public Long id;
 @Column(nullable=false,length=120) public String name;
 @Column(nullable=false,length=500) public String description;
 @Column(nullable=false,precision=12,scale=2) public BigDecimal price;
 @Column(nullable=false) public Integer stock;
}
