package org.schoolstock.schoolstock.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An immutable audit record of a stock purchase, written every time
 * {@link org.schoolstock.schoolstock.service.OrderService#recordStockPurchase}
 * is called: the date it was recorded, the item purchased, the price paid and
 * the quantity bought.
 */
@Entity
@Table(name = "stock_purchase_log")
public class StockPurchaseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    public StockPurchaseLog() {}

    public StockPurchaseLog(LocalDate purchaseDate, Long itemId, BigDecimal price, int quantity) {
        this.purchaseDate = purchaseDate;
        this.itemId = itemId;
        this.price = price;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public Long getItemId() {
        return itemId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }
}
