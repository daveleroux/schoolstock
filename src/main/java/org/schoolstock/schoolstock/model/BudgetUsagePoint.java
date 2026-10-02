package org.schoolstock.schoolstock.model;

import java.math.BigDecimal;

/**
 * A single point on the "View Budget" usage chart: the budget remaining as of
 * a given date. Points that correspond to an actual stock purchase also carry
 * the purchase's details (item, quantity, price) so the UI can show them on
 * mouseover; the period's start/end "anchor" points leave these null.
 */
public class BudgetUsagePoint {

    private final String date;
    private final BigDecimal remaining;
    private final String itemName;
    private final Integer quantity;
    private final BigDecimal price;

    public BudgetUsagePoint(String date, BigDecimal remaining, String itemName, Integer quantity, BigDecimal price) {
        this.date = date;
        this.remaining = remaining;
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getDate() {
        return date;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public String getItemName() {
        return itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
