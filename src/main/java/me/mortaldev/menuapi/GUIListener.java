package me.mortaldev.menuapi;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

/**
 * Listens for Bukkit inventory events and delegates them to the {@link GUIManager}.
 */
public class GUIListener implements Listener {

    private final GUIManager guiManager; //

    /**
     * Constructs a new GUIListener with the given GUIManager.
     *
     * @param guiManager The GUIManager instance to handle events.
     */
    public GUIListener(GUIManager guiManager) {
        this.guiManager = guiManager; //
    }

    /**
     * Handles inventory click events. This method is called by the Bukkit event system
     * when a player clicks in an inventory. It delegates the handling to the {@link GUIManager}.
     *
     * @param event The InventoryClickEvent.
     */
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        this.guiManager.handleClick(event); //
    }

    /**
     * Handles inventory open events. This method is called by the Bukkit event system
     * when an inventory is opened. It delegates the handling to the {@link GUIManager}.
     *
     * @param event The InventoryOpenEvent.
     */
    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        this.guiManager.handleOpen(event); //
    }

    /**
     * Handles inventory close events. This method is called by the Bukkit event system
     * when an inventory is closed. It delegates the handling to the {@link GUIManager}.
     *
     * @param event The InventoryCloseEvent.
     */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        this.guiManager.handleClose(event); //
    }
}