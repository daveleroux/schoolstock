package org.schoolstock.schoolstock.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Min(0)
    @Column(nullable = false)
    private int stockQuantity = 0;

    @Column(nullable = false)
    private boolean provisional = false;

    @PositiveOrZero
    @Column(nullable = false)
    private int availableStock = 0;

    public Item() {}

    public Item(String name, String description, int stockQuantity) {
        this.name = name;
        this.description = description;
        this.stockQuantity = stockQuantity;
        this.availableStock = stockQuantity;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public boolean isProvisional() {
        return provisional;
    }

    public void setProvisional(boolean provisional) {
        this.provisional = provisional;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = availableStock;
    }
}
