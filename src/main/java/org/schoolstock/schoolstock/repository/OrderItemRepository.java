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
}
