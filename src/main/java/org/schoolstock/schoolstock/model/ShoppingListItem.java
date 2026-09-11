package org.schoolstock.schoolstock.model;

/**
 * A read-only aggregate row for the stock controller's Shopping List: an
 * {@link Item} that has one or more OrderItems in {@link OrderItemState#AWAITING_STOCK},
 * together with the total quantity required to satisfy all of them.
 */
public class ShoppingListItem {

    private final Item item;
    private final int requiredQuantity;

    public ShoppingListItem(Item item, Long requiredQuantity) {
        this.item = item;
        this.requiredQuantity = requiredQuantity == null ? 0 : requiredQuantity.intValue();
    }

    public Item getItem() {
        return item;
    }

    public int getRequiredQuantity() {
        return requiredQuantity;
    }
}
