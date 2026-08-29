package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.model.Item;
import org.schoolstock.schoolstock.repository.ItemRepository;
import org.schoolstock.schoolstock.service.OrderService;
import org.springframework.data.domain.Sort;
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
import java.util.List;

@Controller
@RequestMapping("/stock")
public class StockController {

    private final OrderService orderService;
    private final ItemRepository itemRepository;

    public StockController(OrderService orderService, ItemRepository itemRepository) {
        this.orderService = orderService;
        this.itemRepository = itemRepository;
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

    @GetMapping("/items/search")
    public String searchItems(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("items", findItems(q));
        return "fragments/stock-item-results :: stock-item-results";
    }

    @PostMapping("/items")
    public String addItem(@RequestParam String name,
                          @RequestParam(defaultValue = "") String description,
                          Model model) {
        orderService.createItem(name.trim(), description.isBlank() ? null : description.trim());
        model.addAttribute("items", findItems(""));
        return "fragments/stock-item-results :: stock-item-results";
    }

    @PostMapping("/items/{id}/edit")
    public String editItem(@PathVariable Long id,
                           @RequestParam String name,
                           @RequestParam(defaultValue = "") String description,
                           Model model) {
        try {
            orderService.updateItemDetails(id, name.trim(), description.isBlank() ? null : description.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        model.addAttribute("items", findItems(""));
        return "fragments/stock-item-results :: stock-item-results";
    }

    @PostMapping("/items/{id}/stock-bought")
    public String stockBought(@PathVariable Long id,
                              @RequestParam String price,
                              @RequestParam int quantity,
                              Model model) {
        try {
            orderService.recordStockPurchase(id, new BigDecimal(price), quantity);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid price value.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        model.addAttribute("items", findItems(""));
        return "fragments/stock-item-results :: stock-item-results";
    }

    @PostMapping("/items/{id}/stock-adjust-up")
    public String stockAdjustUp(@PathVariable Long id,
                                @RequestParam int quantity,
                                Model model) {
        try {
            orderService.adjustStockUp(id, quantity);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        model.addAttribute("items", findItems(""));
        return "fragments/stock-item-results :: stock-item-results";
    }

    @PostMapping("/items/{id}/stock-adjust-down")
    public String stockAdjustDown(@PathVariable Long id,
                                  @RequestParam int quantity,
                                  Model model) {
        try {
            orderService.adjustStockDown(id, quantity);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        model.addAttribute("items", findItems(""));
        return "fragments/stock-item-results :: stock-item-results";
    }

    private List<Item> findItems(String q) {
        return q.isBlank()
                ? itemRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                : itemRepository.search(q);
    }
}
