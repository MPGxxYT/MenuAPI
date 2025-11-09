package me.mortaldev.menuapi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

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

  // Auto-refresh fields
  private BukkitTask autoRefreshTask; //
  private long autoRefreshIntervalTicks = 20L; //
  private boolean autoRefreshEnabled = false; //
  private Plugin plugin; //

  /** Tracks all currently open GUIs by player for refreshing capabilities. */
  private static final Map<Player, InventoryGUI> openGUIs = new ConcurrentHashMap<>(); //

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
   * Sets the inventory for this GUI. This is used internally for special cases
   * like ItemsAdder GUIs where the inventory is created externally.
   *
   * @param inventory The inventory to set.
   */
  protected void setInventory(Inventory inventory) {
    this.inventory = inventory;
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
   * Adds an {@link InventoryButton} to multiple slots in the GUI.
   * The same button will be registered for all specified slots.
   *
   * @param button The InventoryButton to add.
   * @param slots The slot indices where the button will be placed.
   */
  public void addButton(InventoryButton button, int... slots) {
    for (int slot : slots) {
      this.buttonMap.put(slot, button);
    }
  }

  /**
   * Adds an {@link IAButton} to the GUI.
   * Automatically registers the button to all slots it occupies based on its size and anchor.
   *
   * @param iaButton The IAButton to add.
   */
  public void addButton(IAButton iaButton) {
    for (int slot : iaButton.getOccupiedSlots()) {
      this.buttonMap.put(slot, iaButton);
    }
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
   * If the button is an IAButton, all slots occupied by that IAButton will be refreshed.
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

      // If it's an IAButton, refresh all its occupied slots
      if (button instanceof IAButton) {
        IAButton iaButton = (IAButton) button;
        for (int occupiedSlot : iaButton.getOccupiedSlots()) {
          this.inventory.setItem(occupiedSlot, icon);
        }
      } else {
        // Regular button, just refresh this slot
        this.inventory.setItem(slot, icon);
      }
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
   * When the GUI is opened, it calls the {@link #decorate(Player)} method to set up the button icons
   * and starts auto-refresh if enabled.
   *
   * @param event The InventoryOpenEvent.
   */
  @Override
  public void onOpen(InventoryOpenEvent event) {
    Player player = (Player) event.getPlayer();

    // Stop auto-refresh for any previously open GUI by this player
    InventoryGUI previousGUI = openGUIs.get(player);
    if (previousGUI != null && previousGUI != this) {
      previousGUI.stopAutoRefresh();
    }

    // Register this GUI as the active one for this player
    openGUIs.put(player, this);

    this.decorate(player); //
    startAutoRefresh(); //
  }

  /**
   * Handles an inventory close event for this GUI.
   * Stops auto-refresh if it was running.
   * This method can be overridden by subclasses for additional close logic.
   *
   * @param event The InventoryCloseEvent.
   */
  @Override
  public void onClose(InventoryCloseEvent event) {
    Player player = (Player) event.getPlayer();

    // Only remove from openGUIs if this is still the active GUI for this player
    if (openGUIs.get(player) == this) {
      openGUIs.remove(player);
    }

    stopAutoRefresh(); //
  }

  // ========== Auto-Refresh Methods ==========

  /**
   * Enables auto-refresh for this GUI with the specified plugin and interval.
   * Must be called before the GUI is opened to take effect.
   *
   * @param plugin The plugin instance to use for scheduling tasks.
   * @param intervalTicks The interval in ticks between refreshes (20 ticks = 1 second).
   */
  public void enableAutoRefresh(Plugin plugin, long intervalTicks) {
    if (intervalTicks <= 0) {
      throw new IllegalArgumentException("Auto-refresh interval must be positive, got: " + intervalTicks);
    }
    this.plugin = plugin;
    this.autoRefreshIntervalTicks = intervalTicks;
    this.autoRefreshEnabled = true;
  }

  /**
   * Disables auto-refresh for this GUI.
   */
  public void disableAutoRefresh() {
    this.autoRefreshEnabled = false;
    stopAutoRefresh();
  }

  /**
   * Starts the auto-refresh task if enabled.
   * Called automatically when the GUI is opened if auto-refresh is enabled.
   */
  protected void startAutoRefresh() {
    if (!autoRefreshEnabled || plugin == null) {
      return;
    }

    // Stop existing task if any
    stopAutoRefresh();

    autoRefreshTask = Bukkit.getScheduler().runTaskTimer(
        plugin,
        this::autoRefreshTick,
        autoRefreshIntervalTicks,
        autoRefreshIntervalTicks
    );
  }

  /**
   * Stops the auto-refresh task.
   * Called automatically when the GUI is closed.
   */
  protected void stopAutoRefresh() {
    if (autoRefreshTask != null && !autoRefreshTask.isCancelled()) {
      autoRefreshTask.cancel();
      autoRefreshTask = null;
    }
  }

  /**
   * Internal method called by the auto-refresh timer.
   * Validates the player is still online before calling refreshGUI().
   */
  private void autoRefreshTick() {
    Player player;
    try {
      player = getRegisteredPlayer();
    } catch (IllegalStateException e) {
      stopAutoRefresh();
      return;
    }

    if (player == null || !player.isOnline()) {
      stopAutoRefresh();
      return;
    }

    refreshGUI();
  }

  /**
   * Override this method to define how the GUI should refresh when auto-refresh is enabled.
   * By default, this calls refreshButtons() to update all button displays.
   *
   * For more complex GUIs that need to recreate the entire menu (e.g., when data items are added/removed),
   * override this method to reopen the GUI with updated data.
   *
   * <p><b>Example (simple refresh):</b>
   * <pre>{@code
   * @Override
   * protected void refreshGUI() {
   *   refreshButtons(); // Update button displays without recreating inventory
   * }
   * }</pre>
   *
   * <p><b>Example (full refresh):</b>
   * <pre>{@code
   * @Override
   * protected void refreshGUI() {
   *   stopAutoRefresh();
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

  // ========== Static Methods for Global GUI Management ==========

  /**
   * Refreshes all open GUIs of a specific type by calling their refreshGUI() method.
   * Useful for globally updating all players viewing a particular GUI when data changes.
   *
   * <p><b>Example:</b>
   * <pre>{@code
   * // When a lobby is created/destroyed, refresh all LobbiesMenu instances
   * InventoryGUI.refreshAllGUIsOfType(LobbiesMenu.class);
   * }</pre>
   *
   * @param guiClass The class of GUIs to refresh
   */
  public static void refreshAllGUIsOfType(Class<? extends InventoryGUI> guiClass) {
    openGUIs.values().stream()
        .filter(gui -> guiClass.isInstance(gui))
        .forEach(InventoryGUI::refreshGUI);
  }

  /**
   * Refreshes button displays for all open GUIs of a specific type.
   * More efficient than full refresh when only button displays need updating.
   *
   * <p><b>Example:</b>
   * <pre>{@code
   * // Update player counts on all PlayMenu instances
   * InventoryGUI.refreshButtonsForType(PlayMenu.class);
   * }</pre>
   *
   * @param guiClass The class of GUIs to refresh
   */
  public static void refreshButtonsForType(Class<? extends InventoryGUI> guiClass) {
    openGUIs.values().stream()
        .filter(gui -> guiClass.isInstance(gui))
        .forEach(InventoryGUI::refreshButtons);
  }

  /**
   * Gets the currently open GUI for a specific player, if any.
   *
   * @param player The player to check
   * @return The open GUI, or null if the player doesn't have a GUI open
   */
  public static InventoryGUI getOpenGUI(Player player) {
    return openGUIs.get(player);
  }

  /**
   * Checks if a player currently has a GUI open.
   *
   * @param player The player to check
   * @return True if the player has a GUI open, false otherwise
   */
  public static boolean hasOpenGUI(Player player) {
    return openGUIs.containsKey(player);
  }

  /**
   * Abstract method that must be implemented by subclasses to create the actual Bukkit Inventory.
   * This is where the size, title, and initial contents (if any, beyond buttons) of the inventory are defined.
   *
   * @return The created Bukkit Inventory.
   */
  protected abstract Inventory createInventory();
}