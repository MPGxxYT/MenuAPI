package me.mortaldev.menuapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
 * click rules, and runs auto-refresh. Bukkit inventory events reach it through {@link
 * GUIListener}.
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
   * Opens a GUI for a player.
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
    gui.registerPlayer(player);

    // Special handling for ItemsAdder GUIs that use wrappers
    if (gui instanceof ItemsAdderInventoryGUI) {
      ItemsAdderInventoryGUI iaGUI = (ItemsAdderInventoryGUI) gui;
      if (iaGUI.usesWrapper()) {
        // ItemsAdder GUI with wrapper - let it handle opening
        try {
          Object wrapper = iaGUI.createItemsAdderWrapper(player);
          // Use reflection to call showInventory since we can't depend on ItemsAdder
          wrapper.getClass().getMethod("showInventory", Player.class).invoke(wrapper, player);
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(
              "Failed to open ItemsAdder GUI. The wrapper object doesn't have a showInventory method. Make sure you're returning a TexturedInventoryWrapper from createItemsAdderWrapper()",
              e);
        } catch (Exception e) {
          // GUI opened successfully but there was an issue - suppress error since it's working
          // This can happen with ItemsAdder's internal processes
        }
        // The wrapper opened the inventory before it was registered, so the open event was not
        // handled; start the session directly.
        Inventory inventory = player.getOpenInventory().getTopInventory();
        gui.setInventory(inventory);
        guisByInventory.put(inventory, gui);

        List<String> hudIds = iaGUI.getHudIds(player);
        if (!hudIds.isEmpty()) {
          iaGUI.setActiveHudIds(hudIds);
          showHuds(player, hudIds);
        }

        startSession(gui, player, new InventoryOpenEvent(player.getOpenInventory()));
        return;
      }

      // Inventory already shown by ItemsAdder: start the session without reopening it
      Inventory inventory = gui.getInventory();
      if (player.getOpenInventory().getTopInventory() == inventory) {
        guisByInventory.put(inventory, gui);
        startSession(gui, player, new InventoryOpenEvent(player.getOpenInventory()));
        return;
      }
    }

    // Standard GUI opening; the open event starts the session
    guisByInventory.put(gui.getInventory(), gui);
    player.openInventory(gui.getInventory());
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
   * Starts the session for a GUI opened through {@link #openGUI}.
   *
   * @param event The InventoryOpenEvent.
   */
  public void handleOpen(InventoryOpenEvent event) {
    InventoryGUI gui = guisByInventory.get(event.getInventory());
    if (gui == null) {
      return;
    }
    startSession(gui, (Player) event.getPlayer(), event);
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

    // Hide HUDs for ItemsAdder GUIs
    if (gui instanceof ItemsAdderInventoryGUI) {
      ItemsAdderInventoryGUI iaGUI = (ItemsAdderInventoryGUI) gui;
      List<String> hudIds = iaGUI.getActiveHudIds();
      if (!hudIds.isEmpty()) {
        hideHuds(player, hudIds);
      }
    }

    gui.onClose(event);

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

    gui.decorate(player);
    gui.renderButtons(player);
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

  /**
   * Shows HUDs for a player using reflection to avoid ItemsAdder dependency.
   *
   * @param player The player to show HUDs for.
   * @param hudIds List of HUD namespace IDs to show.
   */
  private void showHuds(Player player, List<String> hudIds) {
    try {
      // PlayerHudsHolderWrapper hudsHolder = new PlayerHudsHolderWrapper(player);
      Class<?> holderClass =
          Class.forName("dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper");
      Object hudsHolder = holderClass.getConstructor(Player.class).newInstance(player);

      for (String hudId : hudIds) {
        // PlayerQuantityHudWrapper hud = hudsHolder.getHud(hudId);
        Object hud = holderClass.getMethod("getHud", String.class).invoke(hudsHolder, hudId);
        if (hud != null) {
          // hud.show();
          hud.getClass().getMethod("show").invoke(hud);
        }
      }
    } catch (Exception e) {
      // Silently fail if ItemsAdder classes not found or HUD doesn't exist
      // This is expected if ItemsAdder is not installed or HUD is not configured
    }
  }

  /**
   * Hides HUDs for a player using reflection to avoid ItemsAdder dependency.
   *
   * @param player The player to hide HUDs for.
   * @param hudIds List of HUD namespace IDs to hide.
   */
  private void hideHuds(Player player, List<String> hudIds) {
    try {
      // PlayerHudsHolderWrapper hudsHolder = new PlayerHudsHolderWrapper(player);
      Class<?> holderClass =
          Class.forName("dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper");
      Object hudsHolder = holderClass.getConstructor(Player.class).newInstance(player);

      for (String hudId : hudIds) {
        // PlayerQuantityHudWrapper hud = hudsHolder.getHud(hudId);
        Object hud = holderClass.getMethod("getHud", String.class).invoke(hudsHolder, hudId);
        if (hud != null) {
          // hud.hide();
          hud.getClass().getMethod("hide").invoke(hud);
        }
      }
    } catch (Exception e) {
      // Silently fail if ItemsAdder classes not found or HUD doesn't exist
    }
  }
}
