package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/stock")
public class StockController {

    private final OrderService orderService;

    public StockController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public String pendingOrders(Model model) {
        model.addAttribute("orders", orderService.getPendingOrders());
        return "fragments/pending-orders :: pending-orders";
    }

    @PostMapping("/sub-orders/{id}/deliver")
    public String deliver(@PathVariable Long id, Model model) {
        try {
            orderService.deliverSubOrder(id);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        model.addAttribute("orders", orderService.getPendingOrders());
        return "fragments/pending-orders :: pending-orders";
    }

    @PostMapping("/sub-orders/{id}/cancel")
    public String cancel(@PathVariable Long id, Model model) {
        try {
            orderService.cancelSubOrder(id);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        model.addAttribute("orders", orderService.getPendingOrders());
        return "fragments/pending-orders :: pending-orders";
    }
}
