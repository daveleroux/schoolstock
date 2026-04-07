package org.schoolstock.schoolstock.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sub_orders")
public class SubOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubOrderState state;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false)
    private int sequenceNumber;

    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubOrderItem> items = new ArrayList<>();

    public SubOrder() {}

    public SubOrder(Order order, SubOrderState state, int sequenceNumber) {
        this.order = order;
        this.state = state;
        this.sequenceNumber = sequenceNumber;
    }

    public Long getId() { return id; }

    public Order getOrder() { return order; }

    public void setOrder(Order order) { this.order = order; }

    public SubOrderState getState() { return state; }

    public void setState(SubOrderState state) { this.state = state; }

    public long getVersion() { return version; }

    public int getSequenceNumber() { return sequenceNumber; }

    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public List<SubOrderItem> getItems() { return items; }
}
