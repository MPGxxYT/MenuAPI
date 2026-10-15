package me.mortaldev.menuapi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * A specialized InventoryGUI implementation designed for ItemsAdder custom GUIs. ItemsAdder is
 * accessed through reflection, so this class loads without ItemsAdder installed.
 *
 * <p>This class can be used in two ways:
 *
 * <p>1. Direct usage with an inventory ItemsAdder has already opened:
 *
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
 *     .creator(p -&gt; yourItemStack)
 *     .consumer(e -&gt; yourClickHandler));
 *
 * // Start the session; the inventory is already open, so it is not reopened
 * GUIManager.getInstance().openGUI(gui, player);
 * </pre>
 *
 * <p>2. Extend this class and override {@link #createItemsAdderWrapper(Player)} to create a custom
 * menu class:
 *
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
 *   }
 * }
 *
 * // Usage:
 * GUIManager.getInstance().openGUI(new MyMenu(), player);
 * </pre>
 */
public class ItemsAdderInventoryGUI extends InventoryGUI {

  private final Inventory existingInventory;
  private final List<String> activeHudIds = new ArrayList<>();

  /**
   * Default constructor for subclasses that will override {@link #createItemsAdderWrapper(Player)}.
   */
  public ItemsAdderInventoryGUI() {
    super();
    this.existingInventory = null;
  }

  /**
   * Constructs a new ItemsAdderInventoryGUI with a pre-existing inventory. This inventory should
   * have been created by ItemsAdder's TexturedInventoryWrapper.
   *
   * @param existingInventory The inventory created by ItemsAdder.
   */
  public ItemsAdderInventoryGUI(Inventory existingInventory) {
    super();
    this.existingInventory = existingInventory;
  }

  /**
   * Override this method in subclasses to create your ItemsAdder GUI. This method should:
   *
   * <ol>
   *   <li>Create the FontImageWrapper with your namespace ID
   *   <li>Create the TexturedInventoryWrapper
   *   <li>Return the TexturedInventoryWrapper (do NOT call showInventory)
   * </ol>
   *
   * @param player The player to open the GUI for.
   * @return The TexturedInventoryWrapper that will be used to open the inventory.
   */
  protected Object createItemsAdderWrapper(Player player) {
    throw new UnsupportedOperationException(
        "Either pass an inventory to the constructor or override createItemsAdderWrapper(Player)");
  }

  /**
   * Override this method to specify which HUD animations should be shown with this GUI. Return a
   * list of HUD namespace IDs (e.g., "crusaders:play_gui_animated"). These HUDs are shown when the
   * GUI opens and hidden when it closes.
   *
   * @param player The player viewing the GUI.
   * @return List of HUD IDs to show, or empty list for no HUDs.
   */
  protected List<String> getHudIds(Player player) {
    return new ArrayList<>();
  }

  /**
   * Returns the pre-existing inventory. GUIs that use a wrapper get their inventory when they are
   * opened, so this is not called for them.
   */
  @Override
  protected Inventory createInventory() {
    if (existingInventory != null) {
      return existingInventory;
    }
    throw new UnsupportedOperationException(
        "ItemsAdder GUIs with wrappers get their inventory when opened. This should not be called.");
  }

  /**
   * Checks if this GUI uses an ItemsAdder wrapper (class-based) or a pre-existing inventory.
   *
   * @return True if using a wrapper, false if using a pre-existing inventory.
   */
  public boolean usesWrapper() {
    return existingInventory == null;
  }

  @Override
  Inventory show(Player player) {
    Inventory inventory = usesWrapper() ? showWrapper(player) : showExisting(player);
    if (inventory != null) {
      activeHudIds.clear();
      activeHudIds.addAll(getHudIds(player));
      setHudsVisible(player, activeHudIds, true);
    }
    return inventory;
  }

  @Override
  void hidden(Player player) {
    setHudsVisible(player, activeHudIds, false);
  }

  private Inventory showExisting(Player player) {
    Inventory inventory = getInventory();
    if (player.getOpenInventory().getTopInventory() != inventory) {
      player.openInventory(inventory);
    }
    return inventory;
  }

  private Inventory showWrapper(Player player) {
    Object wrapper = createItemsAdderWrapper(player);
    Method showInventory;
    try {
      showInventory = wrapper.getClass().getMethod("showInventory", Player.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(
          "Failed to open ItemsAdder GUI. The wrapper object doesn't have a showInventory method. Make sure you're returning a TexturedInventoryWrapper from createItemsAdderWrapper()",
          e);
    }

    Inventory before = player.getOpenInventory().getTopInventory();
    try {
      showInventory.invoke(wrapper, player);
    } catch (ReflectiveOperationException e) {
      Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
      if (player.getOpenInventory().getTopInventory() == before) {
        throw new IllegalStateException("ItemsAdder failed to open the GUI.", cause);
      }
      // ItemsAdder can throw from its internal processes after the inventory has opened
      Bukkit.getLogger()
          .log(Level.WARNING, "ItemsAdder reported an error while opening a GUI; it opened anyway.", cause);
    }

    Inventory inventory = player.getOpenInventory().getTopInventory();
    if (inventory == before) {
      // Nothing opened, for example because another plugin cancelled the open
      return null;
    }
    setInventory(inventory);
    return inventory;
  }

  /** Shows or hides HUDs through reflection. Does nothing if ItemsAdder is not installed. */
  private void setHudsVisible(Player player, List<String> hudIds, boolean visible) {
    if (hudIds.isEmpty()) {
      return;
    }
    try {
      // PlayerHudsHolderWrapper hudsHolder = new PlayerHudsHolderWrapper(player);
      Class<?> holderClass =
          Class.forName("dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper");
      Object hudsHolder = holderClass.getConstructor(Player.class).newInstance(player);

      for (String hudId : hudIds) {
        // PlayerQuantityHudWrapper hud = hudsHolder.getHud(hudId);
        Object hud = holderClass.getMethod("getHud", String.class).invoke(hudsHolder, hudId);
        if (hud != null) {
          // hud.show() or hud.hide()
          hud.getClass().getMethod(visible ? "show" : "hide").invoke(hud);
        }
      }
    } catch (Exception e) {
      // Expected if ItemsAdder is not installed or the HUD is not configured
    }
  }
}
