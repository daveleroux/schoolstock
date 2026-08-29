package org.schoolstock.schoolstock.repository;

import org.schoolstock.schoolstock.model.Item;
import org.schoolstock.schoolstock.model.OrderItem;
import org.schoolstock.schoolstock.model.OrderItemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            SELECT DISTINCT oi.item FROM OrderItem oi
            WHERE oi.item.estimatedPrice IS NULL
              AND oi.state = :state
            ORDER BY oi.item.name ASC
            """)
    List<Item> findDistinctItemsWithNullPriceInState(@Param("state") OrderItemState state);

    List<OrderItem> findByStateAndItem(OrderItemState state, Item item);

    /**
     * Order items in the given state for the given item, oldest order first.
     * Used for "first come first served" allocation, e.g. when newly bought
     * stock is used to fulfil order items that have been waiting longest.
     */
    @Query("""
            SELECT oi FROM OrderItem oi
            WHERE oi.state = :state AND oi.item = :item
            ORDER BY oi.order.createdAt ASC, oi.id ASC
            """)
    List<OrderItem> findByStateAndItemOldestFirst(@Param("state") OrderItemState state, @Param("item") Item item);

    /**
     * Order items in the given state for the given item, newest order first.
     * Used when reclaiming already-reserved stock, so that the most recently
     * created orders are affected first.
     */
    @Query("""
            SELECT oi FROM OrderItem oi
            WHERE oi.state = :state AND oi.item = :item
            ORDER BY oi.order.createdAt DESC, oi.id DESC
            """)
    List<OrderItem> findByStateAndItemNewestFirst(@Param("state") OrderItemState state, @Param("item") Item item);
}
