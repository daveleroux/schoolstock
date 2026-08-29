package org.schoolstock.schoolstock.controller;

import org.schoolstock.schoolstock.model.User;
import org.schoolstock.schoolstock.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/approver")
public class ApproverController {

    private final OrderService orderService;

    public ApproverController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public String needsApprovalOrders(@AuthenticationPrincipal User approver, Model model) {
        model.addAttribute("orders", orderService.getOrdersNeedingApprovalFor(approver));
        return "fragments/needs-approval-orders :: needs-approval-orders";
    }

    @PostMapping("/order-items/{id}/approve")
    public String approve(@PathVariable Long id,
                          @AuthenticationPrincipal User approver,
                          Model model) {
        try {
            orderService.approveOrderItem(id);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        model.addAttribute("orders", orderService.getOrdersNeedingApprovalFor(approver));
        return "fragments/needs-approval-orders :: needs-approval-orders";
    }

    @PostMapping("/order-items/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @AuthenticationPrincipal User approver,
                         Model model) {
        try {
            orderService.cancelOrderItem(id);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        model.addAttribute("orders", orderService.getOrdersNeedingApprovalFor(approver));
        return "fragments/needs-approval-orders :: needs-approval-orders";
    }
}
