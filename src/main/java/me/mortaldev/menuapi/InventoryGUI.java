package me.mortaldev.menuapi;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

/**
 * A menu shown to one player as a chest-style inventory. Extend it, say which inventory to show in
 * {@link #createInventory()}, and add buttons in {@link #decorate(Player)}:
 *
 * <pre>{@code
 * public class MainMenu extends InventoryGUI {
 *
 *   @Override
 *   protected Inventory createInventory() {
 *     return Bukkit.createInventory(null, 27, "Main Menu");
 *   }
 *
 *   @Override
 *   public void decorate(Player player) {
 *     addButton(13, new InventoryButton()
 *         .creator(p -> new ItemStack(Material.DIAMOND))
 *         .consumer(event -> event.getWhoClicked().sendMessage("Clicked!")));
 *   }
 * }
 *
 * GUIManager.getInstance().openGUI(new MainMenu(), player);
 * }</pre>
 *
 * <p><b>Lifecycle.</b> {@link GUIManager#openGUI} runs {@link #createInventory()} and opens the
 * inventory, then calls {@link #decorate(Player)}, draws every button, and calls {@link
 * #onOpen(InventoryOpenEvent)}. On each click it applies the click rules, then calls {@link
 * #onClick(InventoryClickEvent)}, then the clicked button's handler. When the inventory closes it
 * calls {@link #onClose(InventoryCloseEvent)} and stops tracking the GUI. Every hook has an empty
 * default, so override only the ones you need, and never call {@code super}.
 *
 * <p><b>One instance, one player.</b> A GUI instance is opened once, for one player. To show it again
 * with its current fields, whether it is still open or has been closed, call {@link #rebuild()}. To
 * show the menu to another player, or from scratch, create a new instance.
 *
 * <p><b>Where things go.</b> Pass services such as managers to the constructor and keep them in
 * fields. Keep state the menu changes itself, such as a page number or filter, in fields too. Add
 * buttons and load data that can change in {@link #decorate(Player)} or {@link #createInventory()},
 * since those run again on every rebuild and the constructor does not.
 *
 * <p><b>Threading.</b> Call every method on the server's main thread.
 */
public abstract class InventoryGUI {

  private final ButtonLayout layout = new ButtonLayout();
  private boolean allowBottomInventoryClick;
  private boolean allowTopInventoryClick;
  private Inventory inventory;
  private Player registeredPlayer;
  private GUIManager manager;
  private boolean building;

  // Auto-refresh settings; the task itself is run by GUIManager
  private long autoRefreshIntervalTicks = 20L;
  private boolean autoRefreshEnabled = false;
  private Plugin plugin;

  /**
   * Creates a GUI that cancels clicks in both its own inventory and the player's inventory. Change
   * that with {@link #allowTopInventoryClick(boolean)} and {@link
   * #allowBottomInventoryClick(boolean)}.
   *
   * <p>The player is not known yet in the constructor, so {@link #getRegisteredPlayer()} and {@link
   * #rebuild()} cannot be called here.
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
  void registerPlayer(Player player, GUIManager manager) {
    if (registeredPlayer != null) {
      throw new IllegalStateException("Player has already been registered.");
    }
    this.registeredPlayer = player;
    this.manager = manager;
  }

  boolean hasRegisteredPlayer() {
    return registeredPlayer != null;
  }

  /**
   * Returns the player this GUI was opened for.
   *
   * <p>The player is set when {@link GUIManager#openGUI} is called, so this works in {@link
   * #createInventory()}, {@link #decorate(Player)}, the hooks, button handlers and {@link
   * #refreshGUI()}, but not in the constructor. It keeps returning the same player after the GUI
   * closes. Inside {@code decorate} and button icon functions, the {@code player} parameter is the
   * same player.
   *
   * @return The player this GUI was opened for.
   * @throws IllegalStateException If the GUI has not been passed to {@link GUIManager#openGUI} yet.
   */
  public Player getRegisteredPlayer() {
    if (registeredPlayer == null) {
      throw new IllegalStateException("Player has not been registered.");
    }
    return registeredPlayer;
  }

  /**
   * Returns the Bukkit inventory this GUI shows.
   *
   * <p>Inside {@link #decorate(Player)}, this is the inventory being decorated, including during a
   * {@link #rebuild()}. Items set directly with {@code getInventory().setItem(...)} are shown but
   * are not buttons: clicking them runs no handler, and a rebuild replaces them.
   *
   * <p>If the GUI has not been opened, the first call runs {@link #createInventory()} and keeps the
   * result. Avoid that in the constructor if {@code createInventory} uses {@link
   * #getRegisteredPlayer()}, which isn't available yet. For an {@link ItemsAdderInventoryGUI} that
   * uses a wrapper, there is no inventory until the GUI opens, and calling this earlier throws.
   *
   * @return The inventory this GUI shows.
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

  /**
   * Called by {@link GUIManager} when this GUI closes, after the onClose hook, and before a rebuild
   * shows it again in a new inventory.
   */
  void hidden(Player player) {}

  /**
   * Creates the inventory for a rebuild of an open GUI. Returns null if the GUI can only get a new
   * inventory by being shown again.
   */
  Inventory createRebuildInventory() {
    return createInventory();
  }

  /** Removes every button and forgets the inventory, so the next show creates a new one. */
  void resetForReopen() {
    layout.clear(null);
    inventory = null;
  }

  /** Removes every button and clears its slots in the current inventory. */
  void clearButtons() {
    layout.clear(inventory);
  }

  boolean isBuilding() {
    return building;
  }

  void setBuilding(boolean building) {
    this.building = building;
  }

  /**
   * Builds this GUI again from its current fields: {@link #createInventory()} and {@link
   * #decorate(Player)} run again, and every button is replaced by the ones {@code decorate} adds.
   * Call it after changing what the menu shows, such as a page number, filter or search query,
   * instead of opening a new instance:
   *
   * <pre>{@code
   * .consumer(event -> {
   *   page++;
   *   rebuild();
   * })
   * }</pre>
   *
   * <p>What the player sees depends on the GUI's state:
   *
   * <ul>
   *   <li>Open, same inventory size: redrawn in the inventory already on screen, so the cursor does
   *       not move. A title change alone is not shown, since an open inventory can't be renamed.
   *   <li>Open, new size: the new inventory opens. The session continues, so {@link
   *       #onOpen(InventoryOpenEvent)} and {@link #onClose(InventoryCloseEvent)} are not called, and
   *       auto-refresh keeps running. If another plugin cancels the new inventory's open, the
   *       session ends.
   *   <li>Closed, for example by an anvil prompt or another menu: opens again for its player as a new
   *       session, and {@code onOpen} is called. This replaces any other menu the player has open,
   *       so check {@code GUIManager.getInstance().getOpenGUI(player) == this} first if the player
   *       may have closed it on purpose.
   *   <li>Player offline: nothing happens.
   * </ul>
   *
   * <p>The constructor does not run again, so data loaded there stays as it was. Load data that must
   * stay current in {@link #createInventory()} or {@link #decorate(Player)}. If a rebuild changes
   * the auto-refresh settings, the refresh task restarts with the new ones.
   *
   * @throws IllegalStateException If this GUI was never passed to {@link GUIManager#openGUI}, or if
   *     this is called from inside {@code createInventory} or {@code decorate}.
   */
  public void rebuild() {
    if (manager == null) {
      throw new IllegalStateException(
          "This GUI has not been opened. Open it with GUIManager.openGUI first.");
    }
    if (building) {
      throw new IllegalStateException("rebuild() cannot be called from createInventory or decorate.");
    }
    manager.rebuild(this);
  }

  /**
   * Sets whether players can click in their own inventory (the bottom half of the screen) while this
   * GUI is open. Clicks there are cancelled by default, which also stops players from shift-clicking
   * items into the menu.
   *
   * <p>Allow it for menus where players move their own items, such as a deposit box. It applies from
   * the next click, so it can be called in the constructor or while the GUI is open. Button handlers
   * never run for clicks in the player's inventory; use {@link #onClick(InventoryClickEvent)} to
   * react to them.
   *
   * <p>A shift-click in the player's inventory counts as a click there, so once this is allowed it
   * can move items into the menu even while {@link #allowTopInventoryClick(boolean)} is {@code
   * false}. Cancel shift-clicks in {@code onClick} if the menu must stay unchanged.
   *
   * @param b {@code true} to allow clicks in the player's inventory, {@code false} to cancel them.
   */
  public void allowBottomInventoryClick(boolean b) {
    this.allowBottomInventoryClick = b;
  }

  /**
   * Sets whether players can click in this GUI's own inventory (the top half of the screen). Clicks
   * there are cancelled by default, so players cannot take button icons.
   *
   * <p>When allowed, players can take and place items in the menu, including button icons. Button
   * handlers still run, and receive an uncancelled event they can cancel themselves. It applies from
   * the next click, so it can be called in the constructor or while the GUI is open.
   *
   * @param b {@code true} to allow clicks in this GUI's inventory, {@code false} to cancel them.
   */
  public void allowTopInventoryClick(boolean b) {
    this.allowTopInventoryClick = b;
  }

  /**
   * Places a button in one slot. Slots count from 0 in the top-left corner, 9 per row in a chest
   * inventory, so a 27-slot GUI has slots 0 to 26.
   *
   * <p>Call this in {@link #decorate(Player)}. Buttons added there are drawn automatically; buttons
   * added anywhere else are not drawn until {@link #refreshButton(int)} or {@link #refreshButtons()}
   * runs, and are removed by the next {@link #rebuild()}. A button already in the slot is replaced.
   * A slot past the end of the inventory is only caught when the GUI is drawn.
   *
   * @param slot The slot to place the button in, 0 or higher.
   * @param button The button to place.
   * @throws IllegalArgumentException If the slot is negative. A slot past the end of the inventory
   *     throws when the GUI is drawn instead.
   */
  public void addButton(int slot, InventoryButton button) {
    layout.place(button, slot);
  }

  /**
   * Places one button in several slots, for example a border of glass panes. Its icon is created
   * once per draw and copied into every slot, and clicking any of them runs the same handler.
   *
   * <p>Otherwise behaves like {@link #addButton(int, InventoryButton)}: call it in {@link
   * #decorate(Player)}, and any button already in one of the slots is replaced there.
   *
   * @param button The button to place.
   * @param slots The slots to place it in, each 0 or higher. Passing none places nothing.
   * @throws IllegalArgumentException If any slot is negative. A slot past the end of the inventory
   *     throws when the GUI is drawn instead.
   */
  public void addButton(InventoryButton button, int... slots) {
    layout.place(button, slots);
  }

  /**
   * Places an area button in every slot its rectangle covers, worked out from its {@link
   * IAButton#size size} and {@link IAButton#anchor anchor} at the moment this is called. Set both
   * before adding it.
   *
   * <p>Otherwise behaves like {@link #addButton(int, InventoryButton)}: call it in {@link
   * #decorate(Player)}, and any button already in a covered slot is replaced there.
   *
   * @param iaButton The area button to place.
   * @throws IllegalArgumentException If the area runs past the right edge of the inventory. An area
   *     that runs past the last row throws when the GUI is drawn instead.
   */
  public void addButton(IAButton iaButton) {
    layout.place(iaButton);
  }

  /**
   * Returns the button in a slot. For an {@link IAButton}, any slot it covers returns the same
   * button.
   *
   * @param slot The slot to look in.
   * @return The button in the slot, or {@code null} if the slot has none.
   */
  public InventoryButton getButton(int slot) {
    return layout.buttonAt(slot);
  }

  /**
   * Removes the button in a slot and clears its icon from the inventory. If it is an {@link
   * IAButton}, it is removed from every slot it covers. Does nothing if the slot has no button.
   *
   * <p>To remove a button permanently, also stop adding it in {@link #decorate(Player)}, since the
   * next {@link #rebuild()} runs {@code decorate} again.
   *
   * @param slot The slot of the button to remove. For an area button, any slot it covers.
   */
  public void removeButton(int slot) {
    layout.remove(slot, inventory);
  }

  /**
   * Adds this GUI's buttons. Override it and call the {@code addButton} methods here.
   *
   * <p>It runs when the GUI opens and again on every {@link #rebuild()}, after {@link
   * #createInventory()}, with all previous buttons already removed. MenuAPI draws the buttons once
   * this returns, so you don't call {@link #refreshButtons()} here. {@link #getInventory()} returns
   * the inventory being decorated, for setting items that aren't buttons.
   *
   * <p>Read data that can change here, not in the constructor, so each rebuild shows the current
   * state. Calling {@link #rebuild()} from here throws. The default implementation adds nothing.
   *
   * @param player The player the GUI is open for, the same as {@link #getRegisteredPlayer()}.
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

  /**
   * Draws every button again by running each icon function, without running {@link
   * #decorate(Player)} or replacing the inventory. Use it when icons change but the buttons and
   * their slots stay the same, for example a live player count.
   *
   * <p>Values that {@code decorate} read and captured in an icon function don't change; read them
   * inside the function instead. To change which buttons exist, call {@link #rebuild()}. Does
   * nothing if the GUI has never been opened.
   *
   * @throws IllegalArgumentException If a button is placed outside the inventory.
   */
  public void refreshButtons() {
    if (registeredPlayer == null || inventory == null) {
      return;
    }
    renderButtons(registeredPlayer);
  }

  /**
   * Draws one button again by running its icon function, in every slot that button covers. Also
   * draws a button added outside {@link #decorate(Player)}. Does nothing if the slot has no button or
   * the GUI has never been opened.
   *
   * @param slot The slot of the button to draw. For an area button, any slot it covers.
   * @throws IllegalArgumentException If the button covers a slot outside the inventory.
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
   * Runs on every click while this GUI is open, before the clicked button's handler. Override it to
   * react to clicks that buttons don't cover.
   *
   * <p>It receives every click in the open view: in this GUI's inventory, in the player's inventory,
   * and outside the window, where {@code event.getClickedInventory()} is {@code null}. The click
   * rules have already been applied, so the event is cancelled unless clicks in that inventory are
   * allowed; you can change that with {@code event.setCancelled}. The default implementation does
   * nothing.
   *
   * @param event The click event from Bukkit.
   */
  public void onClick(InventoryClickEvent event) {}

  /**
   * Runs once the GUI is on screen and its buttons are drawn: when it is opened, and when {@link
   * #rebuild()} reopens it after it was closed. A rebuild of a GUI that is still open does not call
   * it. Override it for things like playing a sound.
   *
   * <p>The event is created by MenuAPI for this call. The inventory is already open, so cancelling
   * the event has no effect. The default implementation does nothing.
   *
   * @param event An open event describing the player's current view.
   */
  public void onOpen(InventoryOpenEvent event) {}

  /**
   * Runs when this GUI's inventory closes, before MenuAPI stops tracking it. That includes the
   * player closing it, another menu or inventory opening for the player, the player leaving, and
   * {@code player.closeInventory()}. A rebuild of a GUI that is still open does not call it.
   * Override it to save changes or clean up.
   *
   * <p>After it returns, the GUI's auto-refresh stops. The GUI can still be shown again with {@link
   * #rebuild()}. The default implementation does nothing.
   *
   * @param event The close event from Bukkit.
   */
  public void onClose(InventoryCloseEvent event) {}

  // ========== Auto-Refresh ==========

  /**
   * Calls {@link #refreshGUI()} every {@code intervalTicks} ticks while this GUI is open. Call it in
   * the constructor.
   *
   * <pre>{@code
   * public LiveStatsMenu(Plugin plugin) {
   *   enableAutoRefresh(plugin, 20L);   // every second
   * }
   * }</pre>
   *
   * <p>The task starts when the GUI opens; the first refresh comes one interval later. It stops when
   * the GUI closes, when another menu opens for the player, when the player goes offline, or after
   * {@link #disableAutoRefresh()}. It keeps running through a {@link #rebuild()}.
   *
   * <p>Calling this on a GUI that is already open has no effect until its next rebuild, which
   * restarts the task with the new interval. If {@code plugin} is {@code null}, no task starts.
   *
   * @param plugin Your plugin, which owns the scheduled task.
   * @param intervalTicks Ticks between refreshes, 1 or more. 20 ticks is one second.
   * @throws IllegalArgumentException If {@code intervalTicks} is 0 or negative.
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

  /**
   * Stops auto-refresh. A running task stops at its next tick without refreshing, and the GUI stays
   * open. To start it again, call {@link #enableAutoRefresh(Plugin, long)} and then {@link
   * #rebuild()}.
   */
  public void disableAutoRefresh() {
    this.autoRefreshEnabled = false;
  }

  Plugin getAutoRefreshPlugin() {
    return plugin;
  }

  /**
   * Runs on every auto-refresh tick, and for each matching GUI when {@link
   * GUIManager#refreshAllGUIsOfType} is called. By default it calls {@link #refreshButtons()}, which
   * redraws the icons of the existing buttons.
   *
   * <p>Override it when a refresh needs to do more. When buttons can be added or removed, for
   * example a list that grows, call {@link #rebuild()}; the session and the refresh task continue:
   *
   * <pre>{@code
   * @Override
   * protected void refreshGUI() {
   *   rebuild();
   * }
   * }</pre>
   *
   * <p>To update only some buttons, call {@link #refreshButton(int)} for each. It runs on the
   * server's main thread.
   */
  protected void refreshGUI() {
    refreshButtons();
  }

  /**
   * Returns whether auto-refresh is turned on. This reflects the last call to {@link
   * #enableAutoRefresh} or {@link #disableAutoRefresh}, not whether a task is running right now.
   *
   * @return {@code true} if auto-refresh is enabled.
   */
  public boolean isAutoRefreshEnabled() {
    return autoRefreshEnabled;
  }

  /**
   * Returns the auto-refresh interval set with {@link #enableAutoRefresh}.
   *
   * @return Ticks between refreshes. 20 if never set, even while auto-refresh is disabled.
   */
  public long getAutoRefreshIntervalTicks() {
    return autoRefreshIntervalTicks;
  }

  /**
   * Creates the inventory this GUI shows: its size and title, and any items that aren't buttons,
   * such as background glass. Add buttons in {@link #decorate(Player)} instead.
   *
   * <pre>{@code
   * @Override
   * protected Inventory createInventory() {
   *   return Bukkit.createInventory(null, 27, "Main Menu");   // 3 rows
   * }
   * }</pre>
   *
   * <p>It runs when the GUI opens and again on every {@link #rebuild()}, before {@code decorate}, so
   * it can choose a size from current data. Return a new inventory each time. Bukkit requires a
   * chest inventory's size to be a multiple of 9 from 9 to 54; other types, such as {@code
   * Bukkit.createInventory(null, InventoryType.HOPPER, title)}, also work. {@link
   * #getRegisteredPlayer()} works here. Calling {@link #rebuild()} from here throws.
   *
   * @return The inventory to show. Must not be {@code null}.
   */
  protected abstract Inventory createInventory();
}
