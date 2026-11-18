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
 * An {@link InventoryGUI} that shows an ItemsAdder textured inventory, with optional ItemsAdder
 * HUDs while it is open. Buttons, hooks, click rules, auto-refresh and {@link #rebuild()} work as in
 * any other GUI. ItemsAdder is accessed through reflection, so this class loads without ItemsAdder
 * installed; your plugin compiles against ItemsAdder's API for the wrapper classes.
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
   * Creates a GUI that gets its textured inventory from {@link #createItemsAdderWrapper(Player)}.
   * Use this constructor, usually implicitly, in a subclass that overrides that method.
   */
  public ItemsAdderInventoryGUI() {
    super();
    this.existingInventory = null;
  }

  /**
   * Creates a GUI around an inventory ItemsAdder has already opened, so MenuAPI can add buttons to
   * it. Open it with {@link GUIManager#openGUI} as usual; if the inventory is already on the
   * player's screen, it is not opened again.
   *
   * <p>Buttons can be added with {@code addButton} before opening, or in {@link #decorate(Player)}
   * in a subclass. A {@link #rebuild()} keeps this inventory and only redraws the buttons.
   *
   * @param existingInventory The inventory from {@code TexturedInventoryWrapper}, usually {@code
   *     player.getOpenInventory().getTopInventory()} right after calling its {@code showInventory}.
   *     Passing {@code null} makes this GUI use {@link #createItemsAdderWrapper(Player)} instead.
   */
  public ItemsAdderInventoryGUI(Inventory existingInventory) {
    super();
    this.existingInventory = existingInventory;
  }

  /**
   * Creates the ItemsAdder wrapper this GUI opens. Override it in a subclass that uses the
   * no-argument constructor:
   *
   * <ol>
   *   <li>Create a {@code FontImageWrapper} with your texture's namespaced ID.
   *   <li>Create a {@code TexturedInventoryWrapper} with it.
   *   <li>Return the wrapper without calling {@code showInventory}; MenuAPI calls it for you.
   * </ol>
   *
   * <p>It runs every time the GUI opens, and on every {@link #rebuild()}, so return a new wrapper
   * each time. The return type is {@code Object} so MenuAPI needs no compile-time dependency on
   * ItemsAdder; it finds {@code showInventory(Player)} through reflection.
   *
   * <p>Opening fails with an {@link IllegalStateException} from {@link GUIManager#openGUI} if the
   * returned object has no public {@code showInventory(Player)} method, or if that method throws
   * without opening anything. If it opens nothing and doesn't throw, no session starts.
   *
   * @param player The player the GUI is opening for.
   * @return A new {@code TexturedInventoryWrapper}.
   * @throws UnsupportedOperationException If not overridden. The default exists only for GUIs
   *     created with an existing inventory, which never call it.
   */
  protected Object createItemsAdderWrapper(Player player) {
    throw new UnsupportedOperationException(
        "Either pass an inventory to the constructor or override createItemsAdderWrapper(Player)");
  }

  /**
   * Lists the ItemsAdder HUDs to show while this GUI is open. Override it to return their namespaced
   * IDs, for example {@code List.of("crusaders:play_gui_animated")}.
   *
   * <p>It runs each time the GUI is shown: on open, and on a {@link #rebuild()} that opens a new
   * inventory. The HUDs are shown then and hidden when the GUI closes or is shown again. An ID that
   * doesn't exist, or a server without ItemsAdder, is ignored without an error.
   *
   * @param player The player viewing the GUI.
   * @return HUD IDs to show. The default is an empty list, for no HUDs.
   */
  protected List<String> getHudIds(Player player) {
    return new ArrayList<>();
  }

  /**
   * Returns the inventory passed to the constructor. You don't override this in an ItemsAdder GUI:
   * a GUI that uses a wrapper gets its inventory from the wrapper when it opens.
   *
   * @return The existing inventory passed to the constructor.
   * @throws UnsupportedOperationException If this GUI uses a wrapper, which has no inventory before
   *     it opens. This also means {@link #getInventory()} throws until then.
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
   * Returns whether this GUI opens its inventory through {@link #createItemsAdderWrapper(Player)},
   * rather than using an inventory passed to the constructor.
   *
   * @return {@code true} if it uses a wrapper, {@code false} if it was given an existing inventory.
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

  /**
   * A wrapped inventory is kept, so a rebuild only redraws its buttons. A GUI that uses a wrapper
   * returns null, so a rebuild creates a new wrapper and shows it again.
   */
  @Override
  Inventory createRebuildInventory() {
    return usesWrapper() ? null : getInventory();
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
