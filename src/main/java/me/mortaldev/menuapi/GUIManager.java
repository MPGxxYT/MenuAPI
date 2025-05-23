package me.mortaldev.menuapi;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

/**
 * Manages active custom inventories and dispatches inventory events to their respective handlers.
 * This class implements the Singleton design pattern.
 */
public class GUIManager {

  /**
   * Inner class to hold the single instance of GUIManager.
   */
  private static class Singleton {
    private static final GUIManager INSTANCE = new GUIManager(); //
  }

  /**
   * Returns the singleton instance of the GUIManager.
   *
   * @return The GUIManager instance.
   */
  public static GUIManager getInstance() {
    return Singleton.INSTANCE; //
  }

  /**
   * Private constructor to prevent direct instantiation.
   */
  private GUIManager() {}

  private final Map<Inventory, InventoryHandler> activeInventories = new HashMap<>(); //

  /**
   * Opens a custom GUI for a player.
   * The GUI is registered with the manager and then opened for the player.
   *
   * @param gui The InventoryGUI to open.
   * @param player The player to open the GUI for.
   */
  public void openGUI(InventoryGUI gui, Player player) {
    gui.registerPlayer(player); //
    this.registerHandledInventory(gui.getInventory(), gui); //
    player.openInventory(gui.getInventory()); //
  }

  /**
   * Registers an inventory with its corresponding handler.
   * This allows the GUIManager to dispatch events to the correct {@link InventoryHandler}.
   *
   * @param inventory The inventory to register.
   * @param handler The handler for the inventory.
   */
  public void registerHandledInventory(Inventory inventory, InventoryHandler handler) {
    this.activeInventories.put(inventory, handler); //
  }

  /**
   * Unregisters an inventory, removing it from the active inventories.
   *
   * @param inventory The inventory to unregister.
   */
  public void unregisterInventory(Inventory inventory) {
    this.activeInventories.remove(inventory); //
  }

  /**
   * Handles an inventory click event. It retrieves the appropriate {@link InventoryHandler}
   * for the clicked inventory and dispatches the event.
   *
   * @param event The InventoryClickEvent.
   */
  public void handleClick(InventoryClickEvent event) {
    InventoryHandler handler = this.activeInventories.get(event.getInventory()); //
    if (handler != null) { //
      handler.onClick(event); //
    }
  }

  /**
   * Handles an inventory open event. It retrieves the appropriate {@link InventoryHandler}
   * for the opened inventory and dispatches the event.
   *
   * @param event The InventoryOpenEvent.
   */
  public void handleOpen(InventoryOpenEvent event) {
    InventoryHandler handler = this.activeInventories.get(event.getInventory()); //
    if (handler != null) { //
      handler.onOpen(event); //
    }
  }

  /**
   * Handles an inventory close event. It retrieves the appropriate {@link InventoryHandler}
   * for the closed inventory, dispatches the event, and then unregisters the inventory.
   *
   * @param event The InventoryCloseEvent.
   */
  public void handleClose(InventoryCloseEvent event) {
    Inventory inventory = event.getInventory(); //
    InventoryHandler handler = this.activeInventories.get(inventory); //
    if (handler != null) { //
      handler.onClose(event); //
      this.unregisterInventory(inventory); //
    }
  }
}