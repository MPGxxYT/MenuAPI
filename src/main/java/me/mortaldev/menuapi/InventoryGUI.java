package me.mortaldev.menuapi;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

/**
 * Abstract base class for creating custom inventory GUIs.
 *
 * <p>Subclasses implement {@link #createInventory()} and add their buttons in {@link
 * #decorate(Player)}. The session itself (tracking, drawing buttons, click rules and auto-refresh)
 * is run by {@link GUIManager}, so {@link #decorate(Player)}, {@link #onOpen(InventoryOpenEvent)},
 * {@link #onClick(InventoryClickEvent)} and {@link #onClose(InventoryCloseEvent)} are optional
 * hooks and do not need to call {@code super}.
 *
 * <p>A GUI instance can be opened once. To show the same menu again, create a new instance.
 */
public abstract class InventoryGUI {

  private final ButtonLayout layout = new ButtonLayout();
  private boolean allowBottomInventoryClick;
  private boolean allowTopInventoryClick;
  private Inventory inventory;
  private Player registeredPlayer;

  // Auto-refresh settings; the task itself is run by GUIManager
  private long autoRefreshIntervalTicks = 20L;
  private boolean autoRefreshEnabled = false;
  private Plugin plugin;

  /**
   * Constructs a new InventoryGUI. By default, clicks in both the bottom (player) and top (GUI)
   * inventories are cancelled.
   */
  public InventoryGUI() {
    allowBottomInventoryClick = false;
    allowTopInventoryClick = false;
  }

  /**
   * Registers the player this GUI is opened for. Called by {@link GUIManager#openGUI}.
   *
   * @throws IllegalStateException If a player has already been registered.
   */
  void registerPlayer(Player player) {
    if (registeredPlayer != null) {
      throw new IllegalStateException("Player has already been registered.");
    }
    this.registeredPlayer = player;
  }

  boolean hasRegisteredPlayer() {
    return registeredPlayer != null;
  }

  /**
   * Gets the player this GUI was opened for. Cannot be called inside the constructor; it is
   * available from {@link #createInventory()} onwards.
   *
   * @return The registered player.
   * @throws IllegalStateException If no player has been registered yet.
   */
  public Player getRegisteredPlayer() {
    if (registeredPlayer == null) {
      throw new IllegalStateException("Player has not been registered.");
    }
    return registeredPlayer;
  }

  /**
   * Returns the Bukkit Inventory object associated with this GUI. The inventory is created on the
   * first call to this method using {@link #createInventory()}.
   *
   * @return The Bukkit Inventory.
   */
  public Inventory getInventory() {
    if (inventory == null) {
      inventory = createInventory();
    }
    return inventory;
  }

  /** Sets an inventory created outside this GUI, such as one opened by ItemsAdder. */
  void setInventory(Inventory inventory) {
    this.inventory = inventory;
  }

  /**
   * Puts this GUI's inventory on screen for the player. Called by {@link GUIManager#openGUI}.
   *
   * @return The inventory now on screen, or null if nothing was opened.
   */
  Inventory show(Player player) {
    Inventory inventory = getInventory();
    player.openInventory(inventory);
    return inventory;
  }

  /** Called by {@link GUIManager} when this GUI closes, after the onClose hook. */
  void hidden(Player player) {}

  /**
   * Sets whether clicks in the player's bottom inventory are allowed.
   *
   * @param b True to allow clicks, false to cancel them.
   */
  public void allowBottomInventoryClick(boolean b) {
    this.allowBottomInventoryClick = b;
  }

  /**
   * Sets whether clicks in the top inventory (the GUI itself) are allowed. Button handlers still run
   * and can cancel the click themselves.
   *
   * @param b True to allow clicks, false to cancel them.
   */
  public void allowTopInventoryClick(boolean b) {
    this.allowTopInventoryClick = b;
  }

  /**
   * Adds an {@link InventoryButton} to a specific slot in the GUI.
   *
   * @param slot The slot index where the button will be placed.
   * @param button The InventoryButton to add.
   */
  public void addButton(int slot, InventoryButton button) {
    layout.place(button, slot);
  }

  /**
   * Adds an {@link InventoryButton} to multiple slots in the GUI. Its icon is created once per draw
   * and placed in every slot.
   *
   * @param button The InventoryButton to add.
   * @param slots The slot indices where the button will be placed.
   */
  public void addButton(InventoryButton button, int... slots) {
    layout.place(button, slots);
  }

  /**
   * Adds an {@link IAButton} to every slot it occupies, based on its size and anchor.
   *
   * @param iaButton The IAButton to add.
   * @throws IllegalArgumentException If the area runs past the right edge of the inventory.
   */
  public void addButton(IAButton iaButton) {
    layout.place(iaButton);
  }

  /**
   * Gets the button at a slot.
   *
   * @param slot The slot to check.
   * @return The button, or null if the slot has none.
   */
  public InventoryButton getButton(int slot) {
    return layout.buttonAt(slot);
  }

  /**
   * Removes the button at a slot and clears its icon. If it is an IAButton, it is removed from
   * every slot it covers.
   *
   * @param slot The slot of the button to remove.
   */
  public void removeButton(int slot) {
    layout.remove(slot, inventory);
  }

  /**
   * Hook for adding buttons. Called once when the GUI opens; {@link GUIManager} draws the buttons
   * into the inventory after this returns. The default implementation does nothing.
   *
   * @param player The player the GUI is being opened for.
   */
  public void decorate(Player player) {}

  /**
   * Draws every button's icon into the inventory for the given player.
   *
   * @throws IllegalArgumentException If a button is placed outside the inventory.
   */
  void renderButtons(Player player) {
    layout.render(player, inventory);
  }

  /** Redraws all button icons for the registered player without recreating the inventory. */
  public void refreshButtons() {
    if (registeredPlayer == null || inventory == null) {
      return;
    }
    renderButtons(registeredPlayer);
  }

  /**
   * Redraws the button at the given slot, in every slot that button covers.
   *
   * @param slot The slot of the button to refresh.
   */
  public void refreshButton(int slot) {
    if (registeredPlayer == null || inventory == null) {
      return;
    }
    layout.render(registeredPlayer, inventory, slot);
  }

  /** Cancels the click if it lands in an inventory this GUI does not allow clicks in. */
  void applyClickRules(InventoryClickEvent event) {
    if (event.getView().getBottomInventory() == event.getClickedInventory()
        && !allowBottomInventoryClick) {
      event.setCancelled(true);
    } else if (event.getView().getTopInventory() == event.getClickedInventory()
        && !allowTopInventoryClick) {
      event.setCancelled(true);
    }
  }

  /** Runs the handler of the button at the clicked slot, if the click is in the top inventory. */
  void clickButton(InventoryClickEvent event) {
    if (event.getView().getTopInventory() != event.getClickedInventory()) {
      return;
    }
    InventoryButton button = layout.buttonAt(event.getSlot());
    if (button != null && button.getEventConsumer() != null) {
      button.getEventConsumer().accept(event);
    }
  }

  /**
   * Hook called on every click while this GUI is open, after the click rules have been applied and
   * before the clicked button's handler runs. The default implementation does nothing.
   *
   * @param event The InventoryClickEvent.
   */
  public void onClick(InventoryClickEvent event) {}

  /**
   * Hook called after the GUI has opened and its buttons have been drawn. The inventory is already
   * on screen, so cancelling the event has no effect. The default implementation does nothing.
   *
   * @param event The InventoryOpenEvent.
   */
  public void onOpen(InventoryOpenEvent event) {}

  /**
   * Hook called when the GUI closes, before its session ends. The default implementation does
   * nothing.
   *
   * @param event The InventoryCloseEvent.
   */
  public void onClose(InventoryCloseEvent event) {}

  // ========== Auto-Refresh ==========

  /**
   * Enables auto-refresh for this GUI. Must be called before the GUI is opened to take effect.
   * {@link GUIManager} starts the refresh task when the GUI opens and stops it when the GUI closes.
   *
   * @param plugin The plugin instance to use for scheduling tasks.
   * @param intervalTicks The interval in ticks between refreshes (20 ticks = 1 second).
   */
  public void enableAutoRefresh(Plugin plugin, long intervalTicks) {
    if (intervalTicks <= 0) {
      throw new IllegalArgumentException(
          "Auto-refresh interval must be positive, got: " + intervalTicks);
    }
    this.plugin = plugin;
    this.autoRefreshIntervalTicks = intervalTicks;
    this.autoRefreshEnabled = true;
  }

  /** Disables auto-refresh. A running refresh task stops before its next refresh. */
  public void disableAutoRefresh() {
    this.autoRefreshEnabled = false;
  }

  Plugin getAutoRefreshPlugin() {
    return plugin;
  }

  /**
   * Called on every auto-refresh tick, and by {@link GUIManager#refreshAllGUIsOfType}. By default
   * this calls {@link #refreshButtons()}.
   *
   * <p>For GUIs that need to be rebuilt (for example when items are added or removed), override this
   * to open a new instance. The current session, including its refresh task, ends automatically:
   *
   * <pre>{@code
   * @Override
   * protected void refreshGUI() {
   *   GUIManager.getInstance().openGUI(new MyMenu(updatedData), getRegisteredPlayer());
   * }
   * }</pre>
   */
  protected void refreshGUI() {
    refreshButtons();
  }

  /**
   * Checks if auto-refresh is currently enabled for this GUI.
   *
   * @return True if auto-refresh is enabled, false otherwise.
   */
  public boolean isAutoRefreshEnabled() {
    return autoRefreshEnabled;
  }

  /**
   * Gets the auto-refresh interval in ticks.
   *
   * @return The interval in ticks between refreshes.
   */
  public long getAutoRefreshIntervalTicks() {
    return autoRefreshIntervalTicks;
  }

  /**
   * Creates the Bukkit Inventory for this GUI: its size, title, and any contents beyond buttons.
   *
   * @return The created Bukkit Inventory.
   */
  protected abstract Inventory createInventory();
}
