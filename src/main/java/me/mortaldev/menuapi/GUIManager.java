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
 * Runs GUI sessions: opens GUIs, tracks which GUI each player has open, draws buttons, applies
 * click rules, and runs auto-refresh. Click and close events reach it through {@link GUIListener};
 * sessions start in {@link #openGUI}.
 */
public class GUIManager {

  private static class Singleton {
    private static final GUIManager INSTANCE = new GUIManager();
  }

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
   * Opens a GUI for a player and starts its session. If the open is cancelled (for example by
   * another plugin), no session starts.
   *
   * @param gui The InventoryGUI to open.
   * @param player The player to open the GUI for.
   * @throws IllegalStateException If this GUI instance has already been opened.
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
   * Gets the GUI a player currently has open.
   *
   * @param player The player to check.
   * @return The open GUI, or null if the player doesn't have a GUI open.
   */
  public InventoryGUI getOpenGUI(Player player) {
    Session session = sessions.get(player.getUniqueId());
    return session == null ? null : session.gui;
  }

  /**
   * Checks if a player currently has a GUI open.
   *
   * @param player The player to check.
   * @return True if the player has a GUI open.
   */
  public boolean hasOpenGUI(Player player) {
    return sessions.containsKey(player.getUniqueId());
  }

  /**
   * Calls {@link InventoryGUI#refreshGUI()} on every open GUI of the given type.
   *
   * <pre>{@code
   * GUIManager.getInstance().refreshAllGUIsOfType(LobbiesMenu.class);
   * }</pre>
   *
   * @param guiClass The class of GUIs to refresh.
   */
  public void refreshAllGUIsOfType(Class<? extends InventoryGUI> guiClass) {
    for (InventoryGUI gui : openGUIsOfType(guiClass)) {
      gui.refreshGUI();
    }
  }

  /**
   * Redraws button icons on every open GUI of the given type, without calling {@link
   * InventoryGUI#refreshGUI()}.
   *
   * @param guiClass The class of GUIs to refresh.
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
   * Applies the GUI's click rules, then calls its onClick hook, then the clicked button's handler.
   *
   * @param event The InventoryClickEvent.
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
   * Calls the GUI's onClose hook and ends its session.
   *
   * @param event The InventoryCloseEvent.
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
