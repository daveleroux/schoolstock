package org.schoolstock.schoolstock.repository;

import org.schoolstock.schoolstock.model.CartItem;
import org.schoolstock.schoolstock.model.Item;
import org.schoolstock.schoolstock.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndItem(User user, Item item);
}
