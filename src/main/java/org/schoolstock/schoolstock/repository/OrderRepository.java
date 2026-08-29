package org.schoolstock.schoolstock.repository;

import org.schoolstock.schoolstock.model.Order;
import org.schoolstock.schoolstock.model.OrderItemState;
import org.schoolstock.schoolstock.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
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
            LEFT JOIN FETCH o.items
            WHERE EXISTS (
                SELECT i FROM OrderItem i WHERE i.order = o AND i.state = :state
            )
            ORDER BY o.createdAt ASC
            """)
    List<Order> findOrdersWithItemInState(@Param("state") OrderItemState state);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.createdBy IN :orderers
              AND EXISTS (
                  SELECT i FROM OrderItem i WHERE i.order = o AND i.state = :state
              )
            ORDER BY o.createdAt ASC
            """)
    List<Order> findOrdersForOrderersWithItemInState(@Param("orderers") Collection<User> orderers,
                                                      @Param("state") OrderItemState state);
}
