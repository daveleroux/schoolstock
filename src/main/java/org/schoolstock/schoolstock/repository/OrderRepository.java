package org.schoolstock.schoolstock.repository;

import org.schoolstock.schoolstock.model.Order;
import org.schoolstock.schoolstock.model.SubOrderState;
import org.schoolstock.schoolstock.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.subOrders
            WHERE o.createdBy = :user
              AND o.createdAt >= :from
              AND o.createdAt < :to
            ORDER BY o.createdAt DESC
            """)
    List<Order> findOrdersForUser(@Param("user") User user,
                                  @Param("from") Instant from,
                                  @Param("to") Instant to);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.subOrders
            WHERE EXISTS (
                SELECT s FROM SubOrder s WHERE s.order = o AND s.state = :state
            )
            ORDER BY o.createdAt ASC
            """)
    List<Order> findOrdersWithSubOrderInState(@Param("state") SubOrderState state);
}
