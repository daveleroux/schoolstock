package org.schoolstock.schoolstock.service;

import org.schoolstock.schoolstock.model.*;
import org.schoolstock.schoolstock.repository.CartItemRepository;
import org.schoolstock.schoolstock.repository.OrderRepository;
import org.schoolstock.schoolstock.repository.SubOrderItemRepository;
import org.schoolstock.schoolstock.repository.SubOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final SubOrderRepository subOrderRepository;
    private final SubOrderItemRepository subOrderItemRepository;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        SubOrderRepository subOrderRepository,
                        SubOrderItemRepository subOrderItemRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.subOrderRepository = subOrderRepository;
        this.subOrderItemRepository = subOrderItemRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersForUser(User user, LocalDate from, LocalDate to, String stateFilter) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Order> orders = orderRepository.findOrdersForUser(user, fromInstant, toInstant);

        if (!"ALL".equals(stateFilter)) {
            SubOrderState filter = SubOrderState.valueOf(stateFilter);
            orders = orders.stream()
                    .filter(o -> o.getSubOrders().stream().anyMatch(s -> s.getState() == filter))
                    .toList();
        }
        return orders;
    }

    @Transactional(readOnly = true)
    public List<Order> getPendingOrders() {
        return orderRepository.findOrdersWithSubOrderInState(SubOrderState.PACKING);
    }

    @Transactional(readOnly = true)
    public List<Order> getNeedsPricesOrders() {
        return orderRepository.findOrdersWithSubOrderInState(SubOrderState.NEEDS_PRICES);
    }

    public void saveEstimatedPrice(Long subOrderItemId, BigDecimal price) {
        SubOrderItem soi = subOrderItemRepository.findById(subOrderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-order item not found: " + subOrderItemId));
        soi.setEstimatedPrice(price.setScale(2, RoundingMode.HALF_UP));
    }

    public void captureSubOrderPrices(Long subOrderId) {
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-order not found: " + subOrderId));
        if (subOrder.getState() != SubOrderState.NEEDS_PRICES) {
            throw new IllegalStateException("Sub-order is not in the Needs Prices state.");
        }
        boolean allCaptured = subOrder.getItems().stream()
                .allMatch(soi -> soi.getEstimatedPrice() != null);
        if (!allCaptured) {
            throw new IllegalStateException("Not all item prices have been captured.");
        }
        subOrder.setState(SubOrderState.NEEDS_APPROVAL);
    }

    public void deliverSubOrder(Long subOrderId) {
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-order not found: " + subOrderId));
        if (!subOrder.getState().canTransitionTo(SubOrderState.DELIVERED)) {
            throw new IllegalStateException("Cannot deliver sub-order in state: " + subOrder.getState());
        }
        for (SubOrderItem soi : subOrder.getItems()) {
            Item item = soi.getItem();
            // Decrease total stock; availableStock was already decremented when PACKING was created
            item.setStockQuantity(item.getStockQuantity() - soi.getQuantity());
        }
        subOrder.setState(SubOrderState.DELIVERED);
    }

    public void cancelSubOrder(Long subOrderId) {
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-order not found: " + subOrderId));
        SubOrderState current = subOrder.getState();
        if (!current.canTransitionTo(SubOrderState.CANCELLED)) {
            throw new IllegalStateException("Cannot cancel sub-order in state: " + current);
        }
        if (current == SubOrderState.PACKING) {
            for (SubOrderItem soi : subOrder.getItems()) {
                Item item = soi.getItem();
                item.setAvailableStock(item.getAvailableStock() + soi.getQuantity());
            }
        }
        subOrder.setState(SubOrderState.CANCELLED);
    }

    /**
     * Creates an Order from the current user's cart, then clears the cart.
     * Splitting rules:
     *   - quantity ≤ availableStock → entire quantity → PACKING sub-order
     *   - quantity > availableStock > 0 → split into PACKING + NEEDS_PRICES
     *   - availableStock = 0 → entire quantity → NEEDS_PRICES sub-order
     *
     * The persistent availableStock on each Item is decremented by the PACKING quantity.
     */
    public Order createOrder(User user) {
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot create an order from an empty cart.");
        }

        Order order = new Order(Instant.now(), user);

        List<SubOrderItem> pendingItems     = new ArrayList<>();
        List<SubOrderItem> needsPricesItems = new ArrayList<>();

        for (CartItem ci : cartItems) {
            Item item          = ci.getItem();
            int available      = item.getAvailableStock();
            int requested      = ci.getQuantity();
            int pendingQty     = Math.min(requested, available);
            int needsPricesQty = requested - pendingQty;

            if (pendingQty > 0) {
                pendingItems.add(new SubOrderItem(null, item, pendingQty));
                item.setAvailableStock(available - pendingQty);
            }
            if (needsPricesQty > 0) {
                needsPricesItems.add(new SubOrderItem(null, item, needsPricesQty));
            }
        }

        int seq = 1;

        if (!pendingItems.isEmpty()) {
            SubOrder sub = new SubOrder(order, SubOrderState.PACKING, seq++);
            for (SubOrderItem soi : pendingItems) {
                soi.setSubOrder(sub);
                sub.getItems().add(soi);
            }
            order.getSubOrders().add(sub);
        }

        if (!needsPricesItems.isEmpty()) {
            SubOrder sub = new SubOrder(order, SubOrderState.NEEDS_PRICES, seq);
            for (SubOrderItem soi : needsPricesItems) {
                soi.setSubOrder(sub);
                sub.getItems().add(soi);
            }
            order.getSubOrders().add(sub);
        }

        orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);
        return order;
    }
}
