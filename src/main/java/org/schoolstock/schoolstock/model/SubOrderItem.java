package org.schoolstock.schoolstock.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sub_order_items")
public class SubOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_order_id", nullable = false)
    private SubOrder subOrder;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int quantity;

    public SubOrderItem() {}

    public SubOrderItem(SubOrder subOrder, Item item, int quantity) {
        this.subOrder = subOrder;
        this.item = item;
        this.quantity = quantity;
    }

    public Long getId() { return id; }

    public SubOrder getSubOrder() { return subOrder; }

    public void setSubOrder(SubOrder subOrder) { this.subOrder = subOrder; }

    public Item getItem() { return item; }

    public void setItem(Item item) { this.item = item; }

    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }
}
