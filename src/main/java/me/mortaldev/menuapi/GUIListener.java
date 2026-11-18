package me.mortaldev.menuapi;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Passes Bukkit's inventory click and close events to a {@link GUIManager}. Without it, menus still
 * open, but buttons do nothing, clicks are not cancelled, and sessions never end.
 *
 * <p>Register it once, in your plugin's {@code onEnable()}:
 *
 * <pre>{@code
 * getServer().getPluginManager().registerEvents(
 *     new GUIListener(GUIManager.getInstance()), this);
 * }</pre>
 *
 * <p>Register only one listener per manager. Each registered listener passes every event on, so two
 * listeners make every click handler and hook run twice.
 *
 * <p>Inventory drag events are not handled. A player who has an item on their cursor can drag it
 * across a menu's slots unless your plugin cancels {@code InventoryDragEvent} itself.
 */
public class GUIListener implements Listener {

  private final GUIManager guiManager;

  /**
   * Creates a listener that sends events to the given manager. Pass {@link
   * GUIManager#getInstance()}.
   *
   * @param guiManager The manager that runs your menus.
   */
  public GUIListener(GUIManager guiManager) {
    this.guiManager = guiManager;
  }

  /**
   * Called by Bukkit for every inventory click on the server. Clicks in an inventory MenuAPI did not
   * open are ignored. You don't call this yourself.
   *
   * @param event The click event from Bukkit.
   */
  @EventHandler
  public void onClick(InventoryClickEvent event) {
    this.guiManager.handleClick(event);
  }

  /**
   * Called by Bukkit for every inventory close on the server. Closes of an inventory MenuAPI did not
   * open are ignored. You don't call this yourself.
   *
   * @param event The close event from Bukkit.
   */
  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    this.guiManager.handleClose(event);
  }
}
