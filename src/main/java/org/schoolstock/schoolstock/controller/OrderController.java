package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.model.Order;
import org.schoolstock.schoolstock.model.User;
import org.schoolstock.schoolstock.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Controller
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders/create")
    public String createOrder(@AuthenticationPrincipal User user, Model model) {
        Order order;
        try {
            order = orderService.createOrder(user);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        model.addAttribute("cartItems", List.of());
        model.addAttribute("createdOrderId", order.getId());
        return "fragments/cart-contents :: cart-contents";
    }

    @GetMapping("/orders")
    public String listOrders(@RequestParam(required = false) String from,
                             @RequestParam(required = false) String to,
                             @RequestParam(defaultValue = "ALL") String state,
                             @AuthenticationPrincipal User user,
                             Model model) {
        populateOrderModel(user, parseFrom(from), parseTo(to), state, model);
        return "fragments/order-list :: order-list";
    }

    @PostMapping("/orders/sub-orders/{id}/cancel")
    public String cancelSubOrder(@PathVariable Long id,
                                 @RequestParam(required = false) String from,
                                 @RequestParam(required = false) String to,
                                 @RequestParam(defaultValue = "ALL") String state,
                                 @AuthenticationPrincipal User user,
                                 Model model) {
        try {
            orderService.cancelSubOrder(id);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        populateOrderModel(user, parseFrom(from), parseTo(to), state, model);
        return "fragments/order-list :: order-list";
    }

    private void populateOrderModel(User user, LocalDate from, LocalDate to, String state, Model model) {
        model.addAttribute("orders", orderService.getOrdersForUser(user, from, to, state));
        model.addAttribute("selectedState", state);
        model.addAttribute("fromDate", from.toString());
        model.addAttribute("toDate", to.toString());
    }

    private LocalDate parseFrom(String from) {
        return (from != null && !from.isBlank()) ? LocalDate.parse(from) : LocalDate.now().minusMonths(3);
    }

    private LocalDate parseTo(String to) {
        return (to != null && !to.isBlank()) ? LocalDate.parse(to) : LocalDate.now();
    }
}
