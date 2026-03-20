package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.model.User;
import org.schoolstock.schoolstock.repository.ItemRepository;
import org.schoolstock.schoolstock.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ItemController {

    private final ItemRepository itemRepository;
    private final CartService cartService;

    public ItemController(ItemRepository itemRepository, CartService cartService) {
        this.itemRepository = itemRepository;
        this.cartService = cartService;
    }

    @GetMapping("/items/search")
    public String search(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("items", q.isBlank() ? List.of() : itemRepository.search(q));
        return "fragments/item-results :: item-results";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long itemId,
                            @AuthenticationPrincipal User user,
                            Model model) {
        cartService.addToCart(user, itemId);
        model.addAttribute("cartItems", cartService.getCartItems(user));
        return "fragments/cart-contents :: cart-contents";
    }

    @PostMapping("/cart/{id}/remove")
    public String remove(@PathVariable Long id,
                         @AuthenticationPrincipal User user,
                         Model model) {
        cartService.removeFromCart(user, id);
        model.addAttribute("cartItems", cartService.getCartItems(user));
        return "fragments/cart-contents :: cart-contents";
    }

    @PostMapping("/cart/{id}/increase")
    public String increase(@PathVariable Long id,
                           @AuthenticationPrincipal User user,
                           Model model) {
        cartService.increaseQuantity(user, id);
        model.addAttribute("cartItems", cartService.getCartItems(user));
        return "fragments/cart-contents :: cart-contents";
    }

    @PostMapping("/cart/{id}/decrease")
    public String decrease(@PathVariable Long id,
                           @AuthenticationPrincipal User user,
                           Model model) {
        cartService.decreaseQuantity(user, id);
        model.addAttribute("cartItems", cartService.getCartItems(user));
        return "fragments/cart-contents :: cart-contents";
    }
}
