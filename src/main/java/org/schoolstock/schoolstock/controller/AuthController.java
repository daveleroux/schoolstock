package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.model.User;
import org.schoolstock.schoolstock.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    private final CartService cartService;

    public AuthController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("cartItems", cartService.getCartItems(user));
        return "home";
    }
}
