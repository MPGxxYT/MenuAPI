package me.mortaldev.menuapi;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * A specialized InventoryGUI implementation designed for ItemsAdder custom GUIs.
 * This class can be used in two ways:
 *
 * 1. Direct usage with a pre-existing inventory:
 * <pre>
 * // Create ItemsAdder GUI
 * FontImageWrapper fontImageWrapper = new FontImageWrapper("namespace:gui_id");
 * TexturedInventoryWrapper wrapper = new TexturedInventoryWrapper(null, 54, "", 32, -48, fontImageWrapper);
 * wrapper.showInventory(player);
 * Inventory inventory = player.getOpenInventory().getTopInventory();
 *
 * // Create MenuAPI wrapper
 * ItemsAdderInventoryGUI gui = new ItemsAdderInventoryGUI(inventory);
 * gui.addButton(10, new InventoryButton()
 *     .creator(p -> yourItemStack)
 *     .consumer(e -> yourClickHandler));
 *
 * // Register with GUIManager
 * GUIManager.getInstance().registerHandledInventory(inventory, gui);
 * </pre>
 *
 * 2. Extend this class and override {@link #createItemsAdderWrapper(Player)} to create a custom menu class:
 * <pre>
 * public class MyMenu extends ItemsAdderInventoryGUI {
 *   {@literal @}Override
 *   protected Object createItemsAdderWrapper(Player player) {
 *     FontImageWrapper wrapper = new FontImageWrapper("namespace:my_menu");
 *     TexturedInventoryWrapper inv = new TexturedInventoryWrapper(null, 54, "", 32, -48, wrapper);
 *     return inv;
 *   }
 *
 *   {@literal @}Override
 *   public void decorate(Player player) {
 *     addButton(10, myButton());
 *     super.decorate(player);
 *   }
 * }
 *
 * // Usage:
 * GUIManager.getInstance().openGUI(new MyMenu(), player);
 * </pre>
 */
public class ItemsAdderInventoryGUI extends InventoryGUI {

  private final Inventory existingInventory;
  private final List<String> hudIds = new ArrayList<>();

  /**
   * Default constructor for subclasses that will override {@link #createItemsAdderWrapper(Player)}.
   */
  public ItemsAdderInventoryGUI() {
    super();
    this.existingInventory = null;
  }

  /**
   * Constructs a new ItemsAdderInventoryGUI with a pre-existing inventory.
   * This inventory should have been created by ItemsAdder's TexturedInventoryWrapper.
   *
   * @param existingInventory The inventory created by ItemsAdder.
   */
  public ItemsAdderInventoryGUI(Inventory existingInventory) {
    super();
    this.existingInventory = existingInventory;
  }

  /**
   * Override this method in subclasses to create your ItemsAdder GUI.
   * This method should:
   * 1. Create the FontImageWrapper with your namespace ID
   * 2. Create the TexturedInventoryWrapper
   * 3. Return the TexturedInventoryWrapper (do NOT call showInventory)
   *
   * @param player The player to open the GUI for.
   * @return The TexturedInventoryWrapper that will be used to open the inventory.
   */
  protected Object createItemsAdderWrapper(Player player) {
    throw new UnsupportedOperationException(
        "Either pass an inventory to the constructor or override createItemsAdderWrapper(Player)");
  }

  /**
   * Override this method to specify which HUD animations should be shown with this GUI.
   * Return a list of HUD namespace IDs (e.g., "crusaders:play_gui_animated").
   * These HUDs will be automatically shown when the GUI opens and hidden when it closes.
   *
   * @param player The player viewing the GUI.
   * @return List of HUD IDs to show, or empty list for no HUDs.
   */
  protected List<String> getHudIds(Player player) {
    return new ArrayList<>();
  }

  /**
   * Internal method to store HUD IDs for cleanup.
   */
  void setActiveHudIds(List<String> hudIds) {
    this.hudIds.clear();
    this.hudIds.addAll(hudIds);
  }

  /**
   * Internal method to get active HUD IDs for cleanup.
   */
  List<String> getActiveHudIds() {
    return new ArrayList<>(hudIds);
  }

  /**
   * Returns the inventory for this GUI.
   * For ItemsAdder GUIs with a wrapper, this method handles the special opening logic.
   * For pre-existing inventories, it simply returns the inventory.
   *
   * @return The inventory.
   */
  @Override
  protected Inventory createInventory() {
    if (existingInventory != null) {
      return existingInventory;
    }
    // For class-based ItemsAdder GUIs, we need special handling
    // The actual opening will be done in a custom way
    throw new UnsupportedOperationException(
        "ItemsAdder GUIs with wrappers require special opening. This should not be called.");
  }

  /**
   * Checks if this GUI uses an ItemsAdder wrapper (class-based) or a pre-existing inventory.
   *
   * @return True if using a wrapper, false if using a pre-existing inventory.
   */
  public boolean usesWrapper() {
    return existingInventory == null;
  }
}