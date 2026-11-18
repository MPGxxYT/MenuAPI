package me.mortaldev.menuapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Opens menus and runs them while they are open: it tracks which {@link InventoryGUI} each player
 * has open, draws buttons, applies click rules, calls the GUI's hooks and button handlers, and runs
 * auto-refresh. Get it with {@link #getInstance()}.
 *
 * <pre>{@code
 * GUIManager.getInstance().openGUI(new MainMenu(), player);
 * }</pre>
 *
 * <p>Clicks and closes reach it through a {@link GUIListener}, which your plugin registers once in
 * {@code onEnable()}. A player has at most one MenuAPI menu open at a time, which this class calls a
 * session: it starts when {@link #openGUI} opens a menu and ends when that menu's inventory closes.
 *
 * <p>Every method must be called on the server's main thread. The manager keeps its sessions in
 * plain maps and calls Bukkit inventory methods, neither of which is safe from another thread. To
 * use data loaded asynchronously, switch back with {@code Bukkit.getScheduler().runTask(...)} first.
 */
public class GUIManager {

  private static class Singleton {
    private static final GUIManager INSTANCE = new GUIManager();
  }

  /**
   * Returns the shared manager. Every call returns the same instance, so use it both when
   * registering {@link GUIListener} and when opening menus.
   *
   * <p>If you shade and relocate MenuAPI as the README recommends, each plugin gets its own copy of
   * this class and therefore its own manager, which only sees the menus that plugin opened.
   *
   * @return The manager that runs every menu opened through it.
   */
  public static GUIManager getInstance() {
    return Singleton.INSTANCE;
  }

  /** Package-private so tests can start from an empty manager. */
  GUIManager() {}

  /** The open GUI for one player, and its auto-refresh task if one is running. */
  private static final class Session {
    private final InventoryGUI gui;
    private BukkitTask refreshTask;

    private Session(InventoryGUI gui) {
      this.gui = gui;
    }

    private void stopRefresh() {
      if (refreshTask != null && !refreshTask.isCancelled()) {
        refreshTask.cancel();
      }
      refreshTask = null;
    }
  }

  private final Map<Inventory, InventoryGUI> guisByInventory = new HashMap<>();
  private final Map<UUID, Session> sessions = new HashMap<>();

  /**
   * Opens a menu for a player. This runs the GUI's {@link InventoryGUI#createInventory()}, opens the
   * inventory, calls {@link InventoryGUI#decorate(Player)}, draws the buttons, calls {@link
   * InventoryGUI#onOpen(InventoryOpenEvent)}, and starts auto-refresh if the GUI enabled it.
   *
   * <p>Whatever the player had open is closed first, so a menu already open runs its {@link
   * InventoryGUI#onClose(InventoryCloseEvent)} as usual. There's no need to call {@code
   * player.closeInventory()} beforehand.
   *
   * <p>Each GUI instance can be passed here once, and is tied to that player from then on. To show
   * it again with its current fields, call {@link InventoryGUI#rebuild()} on it; to open the menu
   * fresh or for another player, pass a new instance. If another plugin cancels the inventory open,
   * nothing else happens and the instance still counts as opened.
   *
   * @param gui A GUI instance that has not been opened before.
   * @param player The player to show it to.
   * @throws IllegalStateException If this GUI instance has already been passed to this method.
   */
  public void openGUI(InventoryGUI gui, Player player) {
    if (gui.hasRegisteredPlayer()) {
      throw new IllegalStateException(
          "This GUI has already been opened. Create a new instance to open it again.");
    }
    gui.registerPlayer(player, this);
    open(gui, player);
  }

  /**
   * Builds a GUI again from its current state. Called by {@link InventoryGUI#rebuild()}, which
   * describes the behavior.
   */
  void rebuild(InventoryGUI gui) {
    Player player = gui.getRegisteredPlayer();
    if (!player.isOnline()) {
      return;
    }
    Session session = sessions.get(player.getUniqueId());
    if (session == null || session.gui != gui) {
      // Closed, for example by an anvil prompt: open it again as a new session
      gui.resetForReopen();
      open(gui, player);
      return;
    }

    boolean refreshEnabled = gui.isAutoRefreshEnabled();
    long refreshInterval = gui.getAutoRefreshIntervalTicks();
    Plugin refreshPlugin = gui.getAutoRefreshPlugin();

    Inventory current = gui.getInventory();
    Inventory rebuilt = whileBuilding(gui, gui::createRebuildInventory);
    gui.clearButtons();
    if (rebuilt != null && rebuilt.getSize() == current.getSize()) {
      if (rebuilt != current) {
        current.setContents(rebuilt.getContents());
      }
      decorate(gui, player);
    } else {
      // Unmapping the current inventory first means its close event doesn't end the session
      guisByInventory.remove(current);
      gui.hidden(player);
      gui.setInventory(rebuilt);
      Inventory shown = show(gui, player);
      if (shown == null) {
        endCancelledRebuild(gui, player, session, current);
        return;
      }
      guisByInventory.put(shown, gui);
      decorate(gui, player);
    }

    if (gui.isAutoRefreshEnabled() != refreshEnabled
        || gui.getAutoRefreshIntervalTicks() != refreshInterval
        || gui.getAutoRefreshPlugin() != refreshPlugin) {
      session.stopRefresh();
      startRefresh(session, player.getUniqueId());
    }
  }

  /** The new inventory did not open, so the session ends on the inventory that was showing. */
  private void endCancelledRebuild(
      InventoryGUI gui, Player player, Session session, Inventory current) {
    gui.setInventory(current);
    if (player.getOpenInventory().getTopInventory() == current) {
      // Closing runs the usual close handling, so the player isn't left in an inventory without
      // click rules
      guisByInventory.put(current, gui);
      player.closeInventory();
      return;
    }
    session.stopRefresh();
    sessions.remove(player.getUniqueId());
  }

  /** Shows the GUI and starts a new session, unless the open was cancelled. */
  private void open(InventoryGUI gui, Player player) {
    Inventory inventory = show(gui, player);
    if (inventory == null) {
      return;
    }
    guisByInventory.put(inventory, gui);
    startSession(gui, player, new InventoryOpenEvent(player.getOpenInventory()));
  }

  /** Shows the GUI and returns its inventory, or null if nothing was opened. */
  private Inventory show(InventoryGUI gui, Player player) {
    Inventory inventory = whileBuilding(gui, () -> gui.show(player));
    if (inventory == null || player.getOpenInventory().getTopInventory() != inventory) {
      // The open was cancelled, for example by another plugin
      return null;
    }
    return inventory;
  }

  private void decorate(InventoryGUI gui, Player player) {
    whileBuilding(
        gui,
        () -> {
          gui.decorate(player);
          return null;
        });
    gui.renderButtons(player);
  }

  /** Runs a step that may call createInventory or decorate, where rebuild() is not allowed. */
  private static <T> T whileBuilding(InventoryGUI gui, Supplier<T> step) {
    gui.setBuilding(true);
    try {
      return step.get();
    } finally {
      gui.setBuilding(false);
    }
  }

  /**
   * Returns the MenuAPI menu a player has open. Inventories opened without MenuAPI, such as a chest
   * the player opened, don't count.
   *
   * <pre>{@code
   * if (GUIManager.getInstance().getOpenGUI(player) instanceof ShopMenu shop) {
   *   shop.rebuild();
   * }
   * }</pre>
   *
   * @param player The player to check.
   * @return The open menu, or {@code null} if the player has no MenuAPI menu open.
   */
  public InventoryGUI getOpenGUI(Player player) {
    Session session = sessions.get(player.getUniqueId());
    return session == null ? null : session.gui;
  }

  /**
   * Returns whether a player has a MenuAPI menu open. Inventories opened without MenuAPI don't
   * count. Same as {@code getOpenGUI(player) != null}.
   *
   * @param player The player to check.
   * @return {@code true} if the player has a MenuAPI menu open.
   */
  public boolean hasOpenGUI(Player player) {
    return sessions.containsKey(player.getUniqueId());
  }

  /**
   * Calls {@link InventoryGUI#refreshGUI()} on every open menu of the given class, for every player.
   * Use it when data many players may be viewing changes, for example after a lobby is created:
   *
   * <pre>{@code
   * GUIManager.getInstance().refreshAllGUIsOfType(LobbiesMenu.class);
   * }</pre>
   *
   * <p>By default {@code refreshGUI()} only redraws button icons, the same as {@link
   * #refreshButtonsForType}. Menus whose buttons can change override it to call {@link
   * InventoryGUI#rebuild()}. Subclasses of the given class are included, so passing {@code
   * InventoryGUI.class} refreshes every open menu. Does nothing if none are open.
   *
   * @param guiClass The menu class to refresh.
   */
  public void refreshAllGUIsOfType(Class<? extends InventoryGUI> guiClass) {
    for (InventoryGUI gui : openGUIsOfType(guiClass)) {
      gui.refreshGUI();
    }
  }

  /**
   * Calls {@link InventoryGUI#refreshButtons()} on every open menu of the given class, for every
   * player, without calling {@link InventoryGUI#refreshGUI()}. Use it when icons change but the
   * buttons stay the same, for example a player count shown in a lore line.
   *
   * <p>Subclasses of the given class are included. Does nothing if none are open.
   *
   * @param guiClass The menu class to refresh.
   */
  public void refreshButtonsForType(Class<? extends InventoryGUI> guiClass) {
    for (InventoryGUI gui : openGUIsOfType(guiClass)) {
      gui.refreshButtons();
    }
  }

  // A copy, since refreshGUI() may open a new GUI and change the sessions map
  private List<InventoryGUI> openGUIsOfType(Class<? extends InventoryGUI> guiClass) {
    List<InventoryGUI> guis = new ArrayList<>();
    for (Session session : sessions.values()) {
      if (guiClass.isInstance(session.gui)) {
        guis.add(session.gui);
      }
    }
    return guis;
  }

  /**
   * Handles a click for the menu it belongs to: applies the menu's click rules, then calls {@link
   * InventoryGUI#onClick(InventoryClickEvent)}, then the clicked button's handler. Clicks in an
   * inventory MenuAPI did not open are ignored.
   *
   * <p>{@link GUIListener} calls this for you. Don't call it yourself, or every handler runs again.
   *
   * @param event The click event from Bukkit.
   */
  public void handleClick(InventoryClickEvent event) {
    InventoryGUI gui = guisByInventory.get(event.getInventory());
    if (gui == null) {
      return;
    }
    gui.applyClickRules(event);
    gui.onClick(event);
    gui.clickButton(event);
  }

  /**
   * Handles a close for the menu it belongs to: calls {@link
   * InventoryGUI#onClose(InventoryCloseEvent)}, stops its auto-refresh and ends its session.
   * Closes of an inventory MenuAPI did not open are ignored.
   *
   * <p>{@link GUIListener} calls this for you. To close a menu, call {@code player.closeInventory()}
   * instead.
   *
   * @param event The close event from Bukkit.
   */
  public void handleClose(InventoryCloseEvent event) {
    Inventory inventory = event.getInventory();
    InventoryGUI gui = guisByInventory.get(inventory);
    if (gui == null) {
      return;
    }
    Player player = (Player) event.getPlayer();

    gui.onClose(event);
    gui.hidden(player);

    Session session = sessions.get(player.getUniqueId());
    if (session != null && session.gui == gui) {
      session.stopRefresh();
      sessions.remove(player.getUniqueId());
    }
    guisByInventory.remove(inventory);
  }

  private void startSession(InventoryGUI gui, Player player, InventoryOpenEvent event) {
    Session previous = sessions.get(player.getUniqueId());
    if (previous != null && previous.gui != gui) {
      previous.stopRefresh();
    }
    Session session = new Session(gui);
    sessions.put(player.getUniqueId(), session);

    decorate(gui, player);
    gui.onOpen(event);
    startRefresh(session, player.getUniqueId());
  }

  private void startRefresh(Session session, UUID playerId) {
    InventoryGUI gui = session.gui;
    Plugin plugin = gui.getAutoRefreshPlugin();
    if (!gui.isAutoRefreshEnabled() || plugin == null) {
      return;
    }
    long interval = gui.getAutoRefreshIntervalTicks();
    session.refreshTask =
        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  Player player = Bukkit.getPlayer(playerId);
                  if (!gui.isAutoRefreshEnabled() || player == null || !player.isOnline()) {
                    session.stopRefresh();
                    return;
                  }
                  gui.refreshGUI();
                },
                interval,
                interval);
  }
}
