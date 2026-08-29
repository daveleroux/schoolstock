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

    /**
     * Approves a NEEDS_APPROVAL order item. If the item currently has free
     * (available) stock, the approved quantity is fulfilled straight from it and
     * moves directly to PACKING — split across two order items if only part of
     * the quantity can be covered. Any portion that can't be covered by available
     * stock moves to AWAITING_STOCK, to be fulfilled later by a stock purchase.
     */
    public void approveOrderItem(Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + orderItemId));
        if (orderItem.getState() != OrderItemState.NEEDS_APPROVAL) {
            throw new IllegalStateException("Cannot approve order item in state: " + orderItem.getState());
        }

        Item item = orderItem.getItem();
        int covered = Math.min(item.getAvailableStock(), orderItem.getQuantity());
        if (covered > 0) {
            item.setAvailableStock(item.getAvailableStock() - covered);
            moveQuantityToState(orderItem, covered, OrderItemState.PACKING);
        }
        if (orderItem.getState() == OrderItemState.NEEDS_APPROVAL) {
            // Nothing, or not everything, could be covered by available stock.
            orderItem.setState(OrderItemState.AWAITING_STOCK);
        }
    }

    public void saveEstimatedPrice(Long itemId, BigDecimal price) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        item.setEstimatedPrice(price.setScale(2, RoundingMode.HALF_UP));
        moveNeedsPricesToApproval(item);
    }

    /**
     * Moves every NEEDS_PRICES order item for the given item into NEEDS_APPROVAL.
     * Called whenever the item's estimated price is set or updated, since a price
     * is exactly what a NEEDS_PRICES order item is waiting on.
     */
    private void moveNeedsPricesToApproval(Item item) {
        for (OrderItem orderItem : orderItemRepository.findByStateAndItem(OrderItemState.NEEDS_PRICES, item)) {
            orderItem.setState(OrderItemState.NEEDS_APPROVAL);
        }
    }

    /**
     * Records a stock purchase for an item: increases its total stock and sets
     * its estimated price to the price paid. Any order items currently
     * AWAITING_STOCK for this item are then moved into PACKING on a first-come
     * first-served basis (oldest order first), splitting an order item if the
     * purchased quantity only partially covers it. Any purchased quantity left
     * over after clearing the backlog becomes newly available stock.
     */
    public void recordStockPurchase(Long itemId, BigDecimal price, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        item.setEstimatedPrice(price.setScale(2, RoundingMode.HALF_UP));
        moveNeedsPricesToApproval(item);
        increaseStock(item, quantity);
    }

    /**
     * Adjusts an item's stock upward without recording a price, typically used
     * for stock take-on. Behaves exactly like {@link #recordStockPurchase} in
     * every other respect: it does not touch the estimated price, so it does
     * not move any NEEDS_PRICES order items forward.
     */
    public void adjustStockUp(Long itemId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        increaseStock(item, quantity);
    }

    /**
     * Increases an item's total stock by {@code quantity}. Any order items
     * currently AWAITING_STOCK for this item are then moved into PACKING on a
     * first-come first-served basis (oldest order first), splitting an order
     * item if the added quantity only partially covers it. Any added quantity
     * left over after clearing the backlog becomes newly available stock.
     */
    private void increaseStock(Item item, int quantity) {
        item.setStockQuantity(item.getStockQuantity() + quantity);

        int remaining = quantity;
        for (OrderItem waiting : orderItemRepository.findByStateAndItemOldestFirst(OrderItemState.AWAITING_STOCK, item)) {
            if (remaining <= 0) break;
            remaining = moveQuantityToState(waiting, remaining, OrderItemState.PACKING);
        }
        if (remaining > 0) {
            item.setAvailableStock(item.getAvailableStock() + remaining);
        }
    }

    /**
     * Reduces an item's total stock. The reduction is first taken out of free
     * (available) stock; if that isn't enough, already-reserved PACKING order
     * items are pulled back to NEEDS_APPROVAL to free up the shortfall, with the
     * most recently created orders affected first, splitting an order item if
     * only part of its quantity needs to be reclaimed.
     */
    public void adjustStockDown(Long itemId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        if (quantity > item.getStockQuantity()) {
            throw new IllegalStateException("Cannot reduce stock below zero.");
        }

        int fromAvailable = Math.min(quantity, item.getAvailableStock());
        item.setAvailableStock(item.getAvailableStock() - fromAvailable);

        int remaining = quantity - fromAvailable;
        for (OrderItem packing : orderItemRepository.findByStateAndItemNewestFirst(OrderItemState.PACKING, item)) {
            if (remaining <= 0) break;
            remaining = moveQuantityToState(packing, remaining, OrderItemState.NEEDS_APPROVAL);
        }
        if (remaining > 0) {
            throw new IllegalStateException("Not enough reserved stock to cover this reduction.");
        }

        item.setStockQuantity(item.getStockQuantity() - quantity);
    }

    /**
     * Moves up to {@code qty} units of {@code orderItem} into {@code targetState}.
     * If {@code qty} covers the order item's full quantity, the order item itself
     * transitions. Otherwise, the order item is split: a new order item for the
     * moved quantity is created in {@code targetState}, and the original order
     * item's quantity is reduced by that amount (its state is left unchanged).
     * Returns the portion of {@code qty} that could not be applied — always 0
     * unless the order item's quantity was smaller than {@code qty}.
     */
    private int moveQuantityToState(OrderItem orderItem, int qty, OrderItemState targetState) {
        if (!orderItem.getState().canTransitionTo(targetState)) {
            throw new IllegalStateException(
                    "Cannot move order item from " + orderItem.getState() + " to " + targetState);
        }
        if (qty >= orderItem.getQuantity()) {
            int consumed = orderItem.getQuantity();
            orderItem.setState(targetState);
            return qty - consumed;
        }
        OrderItem moved = new OrderItem(orderItem.getOrder(), orderItem.getItem(), qty, targetState);
        orderItem.getOrder().getItems().add(moved);
        orderItemRepository.save(moved);
        orderItem.setQuantity(orderItem.getQuantity() - qty);
        return 0;
    }

    public Item createItem(String name, String description) {
        Item item = new Item();
        item.setName(name);
        item.setDescription(description);
        item.setProvisional(false);
        return itemRepository.save(item);
    }

    public void updateItemDetails(Long itemId, String name, String description) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        item.setName(name);
        item.setDescription(description);
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
