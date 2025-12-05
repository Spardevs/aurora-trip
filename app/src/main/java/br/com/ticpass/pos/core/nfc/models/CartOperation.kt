package br.com.ticpass.pos.core.nfc.models

/**
 * Enum representing different cart operations
 */
enum class CartOperation {
    /**
     * Set exact quantity for an item (replaces existing quantity)
     * If quantity is 0, removes the item
     */
    SET,
    
    /**
     * Increment item quantity by specified amount
     * If item doesn't exist, adds it with the specified quantity
     * Quantity parameter is required and must be > 0
     */
    INCREMENT,
    
    /**
     * Decrement item quantity by specified amount
     * If quantity reaches 0 or below, removes the item
     * Quantity parameter is required and must be > 0
     */
    DECREMENT,
    
    /**
     * Remove specific item completely from cart
     * Quantity parameter is ignored
     */
    REMOVE,
    
    /**
     * Clear entire cart (removes all items)
     * Product ID and quantity parameters are ignored
     */
    CLEAR
}
