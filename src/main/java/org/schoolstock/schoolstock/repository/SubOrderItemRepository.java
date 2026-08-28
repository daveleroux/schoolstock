package org.schoolstock.schoolstock.repository;

import org.schoolstock.schoolstock.model.Item;
import org.schoolstock.schoolstock.model.SubOrderItem;
import org.schoolstock.schoolstock.model.SubOrderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubOrderItemRepository extends JpaRepository<SubOrderItem, Long> {

    @Query("""
            SELECT DISTINCT soi.item FROM SubOrderItem soi
            WHERE soi.item.estimatedPrice IS NULL
              AND soi.subOrder.state = :state
            ORDER BY soi.item.name ASC
            """)
    List<Item> findDistinctItemsWithNullPriceInState(@Param("state") SubOrderState state);
}
