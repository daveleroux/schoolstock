package org.schoolstock.schoolstock.service;

import org.schoolstock.schoolstock.model.CartItem;
import org.schoolstock.schoolstock.model.User;
import org.schoolstock.schoolstock.repository.CartItemRepository;
import org.schoolstock.schoolstock.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;

    public CartService(CartItemRepository cartItemRepository, ItemRepository itemRepository) {
        this.cartItemRepository = cartItemRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findByUser(user);
    }

    public void addToCart(User user, Long itemId) {
        var item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        cartItemRepository.findByUserAndItem(user, item).ifPresentOrElse(
                ci -> ci.setQuantity(ci.getQuantity() + 1),
                () -> cartItemRepository.save(new CartItem(user, item))
        );
    }

    public void removeFromCart(User user, Long cartItemId) {
        cartItemRepository.findById(cartItemId).ifPresent(ci -> {
            if (ci.getUser().getId().equals(user.getId())) {
                cartItemRepository.delete(ci);
            }
        });
    }

    public void increaseQuantity(User user, Long cartItemId) {
        cartItemRepository.findById(cartItemId).ifPresent(ci -> {
            if (ci.getUser().getId().equals(user.getId())) {
                ci.setQuantity(ci.getQuantity() + 1);
            }
        });
    }

    public void decreaseQuantity(User user, Long cartItemId) {
        cartItemRepository.findById(cartItemId).ifPresent(ci -> {
            if (ci.getUser().getId().equals(user.getId())) {
                if (ci.getQuantity() <= 1) {
                    cartItemRepository.delete(ci);
                } else {
                    ci.setQuantity(ci.getQuantity() - 1);
                }
            }
        });
    }
}
