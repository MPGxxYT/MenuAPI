package me.mortaldev.menuapi;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Abstract base class for creating custom inventory GUIs.
 * Provides functionality for managing buttons, controlling click behavior,
 * and handling inventory events.
 */
public abstract class InventoryGUI implements InventoryHandler {

  private final Map<Integer, InventoryButton> buttonMap = new HashMap<>(); //
  private boolean allowBottomInventoryClick; //
  private boolean allowTopInventoryClick; //
  private Inventory inventory; //
  private Player registeredPlayer; //

  /**
   * Constructs a new InventoryGUI.
   * By default, clicks in both the bottom (player) and top (GUI) inventories are disallowed.
   */
  public InventoryGUI() {
    allowBottomInventoryClick = false; //
    allowTopInventoryClick = false; //
  }

  /**
   * Registers a player with this GUI. This method should be called once when the GUI is
   * being prepared to be opened for a specific player.
   *
   * @param player The player to register.
   * @throws IllegalStateException If a player has already been registered.
   */
  public void registerPlayer(Player player) {
    if (registeredPlayer == null) { //
      this.registeredPlayer = player; //
      return; //
    }
    throw new IllegalStateException("Player has already been registered."); //
  }

  /**
   * Gets the player currently registered with this GUI.
   * This method cannot be called within the constructor. It should be used within the
   * {@link #createInventory()} method or after the player has been registered.
   *
   * @return The registered player.
   * @throws IllegalStateException If no player has been registered yet.
   */
  // Cannot be called inside constructor. Use within createInventory().
  public Player getRegisteredPlayer() {
    if (registeredPlayer == null) { //
      throw new IllegalStateException("Player has not been registered."); //
    }
    return registeredPlayer; //
  }

  /**
   * Returns the Bukkit Inventory object associated with this GUI.
   * The inventory is created on the first call to this method using {@link #createInventory()}.
   *
   * @return The Bukkit Inventory.
   */
  public Inventory getInventory() {
    if (inventory == null) { //
      inventory = createInventory(); //
    }
    return inventory; //
  }

  /**
   * Sets whether clicks in the player's bottom inventory (e.g., hotbar, player inventory) are allowed.
   *
   * @param b True to allow clicks, false to cancel them.
   */
  public void allowBottomInventoryClick(boolean b) {
    this.allowBottomInventoryClick = b; //
  }

  /**
   * Sets whether clicks in the top inventory (the GUI itself) are allowed.
   * Note that even if allowed, specific button actions will still override this for their slots.
   *
   * @param b True to allow clicks, false to cancel them.
   */
  public void allowTopInventoryClick(boolean b) {
    this.allowTopInventoryClick = b; //
  }

  /**
   * Adds an {@link InventoryButton} to a specific slot in the GUI.
   *
   * @param slot The slot index where the button will be placed.
   * @param button The InventoryButton to add.
   */
  public void addButton(int slot, InventoryButton button) {
    this.buttonMap.put(slot, button); //
  }

  /**
   * Decorates the inventory with the icons of the registered buttons.
   * This method iterates through all added buttons and sets their created icons in the inventory.
   *
   * @param player The player for whom the icons should be created (passed to the icon creator).
   */
  public void decorate(Player player) {
    this.buttonMap.forEach( //
        (slot, button) -> { //
          ItemStack icon = button.getIconCreator().apply(player); //
          this.inventory.setItem(slot, icon); //
        });
  }

  /**
   * Refreshes all button icons in the inventory for the registered player.
   * This is useful for updating button displays without recreating the entire inventory.
   * Uses the already registered player to regenerate icons.
   */
  public void refreshButtons() {
    if (registeredPlayer == null || inventory == null) {
      return;
    }
    this.buttonMap.forEach( //
        (slot, button) -> { //
          ItemStack icon = button.getIconCreator().apply(registeredPlayer); //
          this.inventory.setItem(slot, icon); //
        });
  }

  /**
   * Refreshes a specific button at the given slot.
   *
   * @param slot The slot of the button to refresh.
   */
  public void refreshButton(int slot) {
    if (registeredPlayer == null || inventory == null) {
      return;
    }
    InventoryButton button = this.buttonMap.get(slot);
    if (button != null) {
      ItemStack icon = button.getIconCreator().apply(registeredPlayer);
      this.inventory.setItem(slot, icon);
    }
  }

  /**
   * Gets the button map for advanced manipulation.
   *
   * @return The map of slot to InventoryButton.
   */
  protected Map<Integer, InventoryButton> getButtonMap() {
    return buttonMap;
  }

  /**
   * Handles an inventory click event for this GUI.
   * It controls whether clicks in the bottom or top inventory are cancelled based on
   * {@code allowBottomInventoryClick} and {@code allowTopInventoryClick}.
   * If the click is in the top inventory and a button exists at the clicked slot,
   * its event consumer is activated.
   *
   * @param event The InventoryClickEvent.
   */
  @Override
  public void onClick(InventoryClickEvent event) {
    if (event.getView().getBottomInventory() == event.getClickedInventory() //
        && !allowBottomInventoryClick) { //
      event.setCancelled(true); //
    } else if (event.getView().getTopInventory() == event.getClickedInventory() //
        && !allowTopInventoryClick) { //
      event.setCancelled(true); //
    }
    if (event.getView().getTopInventory() == event.getClickedInventory()) { //
      int slot = event.getSlot(); //
      InventoryButton button = this.buttonMap.get(slot); //
      if (button != null) { //
        button.getEventConsumer().accept(event); //
      }
    }
  }

  /**
   * Handles an inventory open event for this GUI.
   * When the GUI is opened, it calls the {@link #decorate(Player)} method to set up the button icons.
   *
   * @param event The InventoryOpenEvent.
   */
  @Override
  public void onOpen(InventoryOpenEvent event) {
    this.decorate((Player) event.getPlayer()); //
  }

  /**
   * Handles an inventory close event for this GUI.
   * This method is empty by default and can be overridden by subclasses for specific close logic.
   *
   * @param event The InventoryCloseEvent.
   */
  @Override
  public void onClose(InventoryCloseEvent event) {} //

  /**
   * Abstract method that must be implemented by subclasses to create the actual Bukkit Inventory.
   * This is where the size, title, and initial contents (if any, beyond buttons) of the inventory are defined.
   *
   * @return The created Bukkit Inventory.
   */
  protected abstract Inventory createInventory();
}