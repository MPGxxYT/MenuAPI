package me.mortaldev.menuapi;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

/**
 * Defines the contract for classes that can handle various inventory events.
 * Implementations of this interface can be registered with the {@link GUIManager}
 * to receive click, open, and close events for specific inventories.
 */
public interface InventoryHandler {

    /**
     * Called when an inventory click event occurs within the handled inventory.
     *
     * @param event The InventoryClickEvent.
     */
    void onClick(InventoryClickEvent event);

    /**
     * Called when the handled inventory is opened.
     *
     * @param event The InventoryOpenEvent.
     */
    void onOpen(InventoryOpenEvent event);

    /**
     * Called when the handled inventory is closed.
     *
     * @param event The InventoryCloseEvent.
     */
    void onClose(InventoryCloseEvent event);
}