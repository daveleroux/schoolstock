package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

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

    @GetMapping("/needs-prices")
    public String needsPrices(Model model) {
        model.addAttribute("items", orderService.getItemsNeedingPrices());
        return "fragments/needs-prices-items :: needs-prices-items";
    }

    @PostMapping("/items/{id}/price")
    public String savePrice(@PathVariable Long id,
                            @RequestParam String price,
                            Model model) {
        try {
            orderService.saveEstimatedPrice(id, new BigDecimal(price));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid price value.");
        }
        model.addAttribute("items", orderService.getItemsNeedingPrices());
        return "fragments/needs-prices-items :: needs-prices-items";
    }

    @PostMapping("/order-items/{id}/deliver")
    public String deliver(@PathVariable Long id, Model model) {
        try {
            orderService.deliverOrderItem(id);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        model.addAttribute("orders", orderService.getPendingOrders());
        return "fragments/pending-orders :: pending-orders";
    }

    @PostMapping("/order-items/{id}/cancel")
    public String cancel(@PathVariable Long id, Model model) {
        try {
            orderService.cancelOrderItem(id);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        model.addAttribute("orders", orderService.getPendingOrders());
        return "fragments/pending-orders :: pending-orders";
    }
}
