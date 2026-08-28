package org.schoolstock.schoolstock.repository;

import org.schoolstock.schoolstock.model.Item;
import org.schoolstock.schoolstock.model.SubOrder;
import org.schoolstock.schoolstock.model.SubOrderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubOrderRepository extends JpaRepository<SubOrder, Long> {

    @Query("""
            SELECT DISTINCT s FROM SubOrder s
            JOIN s.items i
            WHERE s.state = :state AND i.item = :item
            """)
    List<SubOrder> findByStateContainingItem(@Param("state") SubOrderState state, @Param("item") Item item);
}
