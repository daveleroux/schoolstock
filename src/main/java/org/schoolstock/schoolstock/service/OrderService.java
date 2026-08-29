package org.schoolstock.schoolstock.service;

import org.schoolstock.schoolstock.model.*;
import org.schoolstock.schoolstock.repository.CartItemRepository;
import org.schoolstock.schoolstock.repository.ItemRepository;
import org.schoolstock.schoolstock.repository.OrderItemRepository;
import org.schoolstock.schoolstock.repository.OrderRepository;
import org.schoolstock.schoolstock.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        CartItemRepository cartItemRepository,
                        OrderItemRepository orderItemRepository,
                        ItemRepository itemRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersForUser(User user, LocalDate from, LocalDate to, String stateFilter) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Order> orders = orderRepository.findOrdersForUser(user, fromInstant, toInstant);

        if (!"ALL".equals(stateFilter)) {
            OrderItemState filter = OrderItemState.valueOf(stateFilter);
            orders = orders.stream()
                    .filter(o -> o.getItems().stream().anyMatch(i -> i.getState() == filter))
                    .toList();
        }
        return orders;
    }

    @Transactional(readOnly = true)
    public List<Order> getPendingOrders() {
        return orderRepository.findOrdersWithItemInState(OrderItemState.PACKING);
    }

    @Transactional(readOnly = true)
    public List<Item> getItemsNeedingPrices() {
        return orderItemRepository.findDistinctItemsWithNullPriceInState(OrderItemState.NEEDS_PRICES);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersNeedingApprovalFor(User approver) {
        List<User> orderers = userRepository.findByApproversContaining(approver);
        if (orderers.isEmpty()) return List.of();
        return orderRepository.findOrdersForOrderersWithItemInState(orderers, OrderItemState.NEEDS_APPROVAL);
    }

    public void approveOrderItem(Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + orderItemId));
        if (!orderItem.getState().canTransitionTo(OrderItemState.AWAITING_STOCK)) {
            throw new IllegalStateException("Cannot approve order item in state: " + orderItem.getState());
        }
        orderItem.setState(OrderItemState.AWAITING_STOCK);
    }

    public void saveEstimatedPrice(Long itemId, BigDecimal price) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        item.setEstimatedPrice(price.setScale(2, RoundingMode.HALF_UP));

        List<OrderItem> affected = orderItemRepository.findByStateAndItem(OrderItemState.NEEDS_PRICES, item);
        for (OrderItem orderItem : affected) {
            orderItem.setState(OrderItemState.NEEDS_APPROVAL);
        }
    }

    public void deliverOrderItem(Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + orderItemId));
        if (!orderItem.getState().canTransitionTo(OrderItemState.DELIVERED)) {
            throw new IllegalStateException("Cannot deliver order item in state: " + orderItem.getState());
        }
        Item item = orderItem.getItem();
        // Decrease total stock; availableStock was already decremented when PACKING was created
        item.setStockQuantity(item.getStockQuantity() - orderItem.getQuantity());
        orderItem.setState(OrderItemState.DELIVERED);
    }

    public void cancelOrderItem(Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + orderItemId));
        OrderItemState current = orderItem.getState();
        if (!current.canTransitionTo(OrderItemState.CANCELLED)) {
            throw new IllegalStateException("Cannot cancel order item in state: " + current);
        }
        if (current == OrderItemState.PACKING) {
            Item item = orderItem.getItem();
            item.setAvailableStock(item.getAvailableStock() + orderItem.getQuantity());
        }
        orderItem.setState(OrderItemState.CANCELLED);
    }

    /**
     * Creates an Order from the current user's cart, then clears the cart.
     * Splitting rules:
     *   - quantity ≤ availableStock → entire quantity → PACKING order item
     *   - quantity > availableStock > 0 → split into PACKING + NEEDS_PRICES/NEEDS_APPROVAL
     *   - availableStock = 0 → entire quantity → NEEDS_PRICES/NEEDS_APPROVAL order item
     *
     * The persistent availableStock on each Item is decremented by the PACKING quantity.
     */
    public Order createOrder(User user) {
        List<CartItem> cartItems = cartItemRepository.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Cannot create an order from an empty cart.");
        }

        Order order = new Order(Instant.now(), user);

        for (CartItem ci : cartItems) {
            Item item          = ci.getItem();
            int available      = item.getAvailableStock();
            int requested      = ci.getQuantity();
            int pendingQty     = Math.min(requested, available);
            int needsPricesQty = requested - pendingQty;

            if (pendingQty > 0) {
                order.getItems().add(new OrderItem(order, item, pendingQty, OrderItemState.PACKING));
                item.setAvailableStock(available - pendingQty);
            }
            if (needsPricesQty > 0) {
                OrderItemState state = item.getEstimatedPrice() != null
                        ? OrderItemState.NEEDS_APPROVAL
                        : OrderItemState.NEEDS_PRICES;
                order.getItems().add(new OrderItem(order, item, needsPricesQty, state));
            }
        }

        orderRepository.save(order);
        cartItemRepository.deleteAll(cartItems);
        return order;
    }
}
