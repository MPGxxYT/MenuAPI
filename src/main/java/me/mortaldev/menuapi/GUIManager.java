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

    // Special handling for ItemsAdder GUIs that use wrappers
    if (gui instanceof ItemsAdderInventoryGUI) {
      ItemsAdderInventoryGUI iaGUI = (ItemsAdderInventoryGUI) gui;
      if (iaGUI.usesWrapper()) {
        // ItemsAdder GUI with wrapper - let it handle opening
        try {
          Object wrapper = iaGUI.createItemsAdderWrapper(player);
          // Use reflection to call showInventory since we can't depend on ItemsAdder
          wrapper.getClass().getMethod("showInventory", Player.class).invoke(wrapper, player);
          Inventory inventory = player.getOpenInventory().getTopInventory();
          // Set the inventory in the GUI so decorate() can use it
          gui.setInventory(inventory);
          this.registerHandledInventory(inventory, gui);

          // Show HUD animations if specified
          java.util.List<String> hudIds = iaGUI.getHudIds(player);
          if (!hudIds.isEmpty()) {
            iaGUI.setActiveHudIds(hudIds);
            showHuds(player, hudIds);
          }

          // Trigger onOpen to start auto-refresh and call decorate
          InventoryOpenEvent openEvent = new InventoryOpenEvent(player.getOpenInventory());
          gui.onOpen(openEvent);
        } catch (NoSuchMethodException e) {
          throw new RuntimeException("Failed to open ItemsAdder GUI. The wrapper object doesn't have a showInventory method. Make sure you're returning a TexturedInventoryWrapper from createItemsAdderWrapper()", e);
        } catch (Exception e) {
          // GUI opened successfully but there was an issue - suppress error since it's working
          // This can happen with ItemsAdder's internal processes
          Inventory inventory = player.getOpenInventory().getTopInventory();
          gui.setInventory(inventory);
          this.registerHandledInventory(inventory, gui);

          // Show HUD animations if specified
          java.util.List<String> hudIds = iaGUI.getHudIds(player);
          if (!hudIds.isEmpty()) {
            iaGUI.setActiveHudIds(hudIds);
            showHuds(player, hudIds);
          }

          // Trigger onOpen to start auto-refresh and call decorate
          InventoryOpenEvent openEvent = new InventoryOpenEvent(player.getOpenInventory());
          gui.onOpen(openEvent);
        }
        return;
      }
    }

    // Standard GUI opening
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
      // Hide HUDs for ItemsAdder GUIs
      if (handler instanceof ItemsAdderInventoryGUI) {
        ItemsAdderInventoryGUI iaGUI = (ItemsAdderInventoryGUI) handler;
        java.util.List<String> hudIds = iaGUI.getActiveHudIds();
        if (!hudIds.isEmpty()) {
          hideHuds((Player) event.getPlayer(), hudIds);
        }
      }

      handler.onClose(event); //
      this.unregisterInventory(inventory); //
    }
  }

  /**
   * Shows HUDs for a player using reflection to avoid ItemsAdder dependency.
   *
   * @param player The player to show HUDs for.
   * @param hudIds List of HUD namespace IDs to show.
   */
  private void showHuds(Player player, java.util.List<String> hudIds) {
    try {
      // PlayerHudsHolderWrapper hudsHolder = new PlayerHudsHolderWrapper(player);
      Class<?> holderClass = Class.forName("dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper");
      Object hudsHolder = holderClass.getConstructor(Player.class).newInstance(player);

      for (String hudId : hudIds) {
        // PlayerQuantityHudWrapper hud = hudsHolder.getHud(hudId);
        Object hud = holderClass.getMethod("getHud", String.class).invoke(hudsHolder, hudId);
        if (hud != null) {
          // hud.show();
          hud.getClass().getMethod("show").invoke(hud);
        }
      }
    } catch (Exception e) {
      // Silently fail if ItemsAdder classes not found or HUD doesn't exist
      // This is expected if ItemsAdder is not installed or HUD is not configured
    }
  }

  /**
   * Hides HUDs for a player using reflection to avoid ItemsAdder dependency.
   *
   * @param player The player to hide HUDs for.
   * @param hudIds List of HUD namespace IDs to hide.
   */
  private void hideHuds(Player player, java.util.List<String> hudIds) {
    try {
      // PlayerHudsHolderWrapper hudsHolder = new PlayerHudsHolderWrapper(player);
      Class<?> holderClass = Class.forName("dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper");
      Object hudsHolder = holderClass.getConstructor(Player.class).newInstance(player);

      for (String hudId : hudIds) {
        // PlayerQuantityHudWrapper hud = hudsHolder.getHud(hudId);
        Object hud = holderClass.getMethod("getHud", String.class).invoke(hudsHolder, hudId);
        if (hud != null) {
          // hud.hide();
          hud.getClass().getMethod("hide").invoke(hud);
        }
      }
    } catch (Exception e) {
      // Silently fail if ItemsAdder classes not found or HUD doesn't exist
    }
  }
}