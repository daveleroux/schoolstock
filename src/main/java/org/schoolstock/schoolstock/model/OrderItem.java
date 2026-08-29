package org.schoolstock.schoolstock.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderItemState state;

    @Version
    @Column(nullable = false)
    private long version;

    public OrderItem() {}

    public OrderItem(Order order, Item item, int quantity, OrderItemState state) {
        this.order = order;
        this.item = item;
        this.quantity = quantity;
        this.state = state;
    }

    public Long getId() { return id; }

    public Order getOrder() { return order; }

    public void setOrder(Order order) { this.order = order; }

    public Item getItem() { return item; }

    public void setItem(Item item) { this.item = item; }

    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) { this.quantity = quantity; }

    public OrderItemState getState() { return state; }

    public void setState(OrderItemState state) { this.state = state; }

    public long getVersion() { return version; }
}
