package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.model.User;
import org.schoolstock.schoolstock.service.AccountService;
import org.schoolstock.schoolstock.service.CartService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final CartService cartService;
    private final AccountService accountService;

    public AuthController(CartService cartService, AccountService accountService) {
        this.cartService = cartService;
        this.accountService = accountService;
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

    @PostMapping("/account/password")
    public String changePassword(@AuthenticationPrincipal User user,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        var result = accountService.changePassword(
                user.getId(), currentPassword, newPassword, confirmPassword);
        model.addAttribute("passwordSuccess", result.success());
        model.addAttribute("passwordError", result.error());
        model.addAttribute("failedRequirements", result.failedRequirements());
        return "fragments/change-password :: change-password";
    }
}
