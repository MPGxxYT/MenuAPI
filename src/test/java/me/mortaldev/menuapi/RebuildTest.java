package me.mortaldev.menuapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RebuildTest {

  private static final int FILTER_SLOT = 8;

  private ServerMock server;
  private Plugin plugin;
  private GUIManager manager;
  private PlayerMock player;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    plugin = MockBukkit.createMockPlugin();
    manager = new GUIManager();
    server.getPluginManager().registerEvents(new GUIListener(manager), plugin);
    player = server.addPlayer();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  /** One staff member in the test menu's list. */
  private record Staff(String name, boolean admin) {}

  /**
   * Built like a paged staff list: a filter field decides which entries show, the row count follows
   * the number of entries shown, and the filter button changes the field and calls rebuild().
   */
  private static class StaffMenu extends InventoryGUI {
    final List<Staff> source;
    final List<String> calls = new ArrayList<>();
    boolean adminsOnly = false;
    int refreshes = 0;
    Plugin refreshPlugin;
    long refreshTicks;
    boolean rebuildInDecorate = false;

    StaffMenu(List<Staff> source) {
      this.source = source;
    }

    private List<Staff> shown() {
      return source.stream().filter(s -> !adminsOnly || s.admin()).toList();
    }

    @Override
    protected Inventory createInventory() {
      int rows = 1 + (shown().size() + 8) / 9;
      Inventory inventory = Bukkit.createInventory(null, 9 * Math.max(rows, 2), "Staff");
      inventory.setItem(4, new ItemStack(Material.WHITE_STAINED_GLASS_PANE));
      return inventory;
    }

    @Override
    public void decorate(Player player) {
      calls.add("decorate");
      if (rebuildInDecorate) {
        rebuild();
      }
      if (refreshPlugin != null) {
        enableAutoRefresh(refreshPlugin, refreshTicks);
      }
      int slot = 9;
      for (Staff ignored : shown()) {
        addButton(slot++, new InventoryButton().creator(p -> new ItemStack(Material.PLAYER_HEAD)));
      }
      addButton(
          FILTER_SLOT,
          new InventoryButton()
              .creator(p -> new ItemStack(Material.MAP))
              .consumer(
                  e -> {
                    adminsOnly = !adminsOnly;
                    rebuild();
                  }));
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
      calls.add("onOpen");
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
      calls.add("onClose");
    }

    @Override
    protected void refreshGUI() {
      refreshes++;
    }
  }

  private static List<Staff> staff(int admins, int others) {
    List<Staff> list = new ArrayList<>();
    for (int i = 0; i < admins; i++) {
      list.add(new Staff("admin" + i, true));
    }
    for (int i = 0; i < others; i++) {
      list.add(new Staff("mod" + i, false));
    }
    return list;
  }

  private int headCount(Inventory inventory) {
    int heads = 0;
    for (ItemStack item : inventory.getContents()) {
      if (item != null && item.getType() == Material.PLAYER_HEAD) {
        heads++;
      }
    }
    return heads;
  }

  private Inventory top() {
    return player.getOpenInventory().getTopInventory();
  }

  private void clickFilter() {
    server
        .getPluginManager()
        .callEvent(
            new InventoryClickEvent(
                player.getOpenInventory(),
                SlotType.CONTAINER,
                FILTER_SLOT,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL));
  }

  @Test
  void sameSizeRedrawsInTheOpenInventoryWithoutHooks() {
    StaffMenu menu = new StaffMenu(staff(2, 3));
    manager.openGUI(menu, player);
    Inventory before = top();
    menu.calls.clear();

    menu.adminsOnly = true;
    menu.rebuild();

    assertSame(before, top());
    assertSame(before, menu.getInventory());
    assertEquals(2, headCount(before));
    assertEquals(Material.WHITE_STAINED_GLASS_PANE, before.getItem(4).getType());
    assertEquals(List.of("decorate"), menu.calls);
    assertSame(menu, manager.getOpenGUI(player));
  }

  @Test
  void sizeChangeOpensTheNewInventoryAndKeepsTheSession() {
    StaffMenu menu = new StaffMenu(staff(2, 10));
    manager.openGUI(menu, player);
    Inventory before = top();
    assertEquals(27, before.getSize());
    menu.calls.clear();

    menu.adminsOnly = true;
    menu.rebuild();

    assertNotSame(before, top());
    assertSame(top(), menu.getInventory());
    assertEquals(18, top().getSize());
    assertEquals(2, headCount(top()));
    assertEquals(List.of("decorate"), menu.calls);
    assertSame(menu, manager.getOpenGUI(player));
  }

  @Test
  void clicksReachTheNewInventoryAfterASizeChange() {
    StaffMenu menu = new StaffMenu(staff(2, 10));
    manager.openGUI(menu, player);

    clickFilter();
    assertEquals(18, top().getSize());
    clickFilter();

    assertEquals(27, top().getSize());
    assertEquals(12, headCount(top()));
    assertSame(menu, manager.getOpenGUI(player));
  }

  @Test
  void narrowingThenWideningTheFilterShowsTheFullListAgain() {
    StaffMenu menu = new StaffMenu(staff(2, 3));
    manager.openGUI(menu, player);

    clickFilter();
    assertEquals(2, headCount(top()));
    clickFilter();

    assertEquals(5, headCount(top()));
  }

  @Test
  void refreshTaskKeepsRunningThroughARebuild() {
    StaffMenu menu = new StaffMenu(staff(2, 10));
    menu.enableAutoRefresh(plugin, 10L);
    manager.openGUI(menu, player);

    menu.adminsOnly = true;
    menu.rebuild();
    server.getScheduler().performTicks(30L);

    assertEquals(3, menu.refreshes);
  }

  @Test
  void refreshTaskRestartsWhenTheRebuildChangesItsInterval() {
    StaffMenu menu = new StaffMenu(staff(2, 3));
    menu.refreshPlugin = plugin;
    menu.refreshTicks = 10L;
    manager.openGUI(menu, player);

    menu.refreshTicks = 5L;
    menu.rebuild();
    server.getScheduler().performTicks(30L);

    assertEquals(6, menu.refreshes);
  }

  @Test
  void cancelledReopenEndsTheSession() {
    StaffMenu menu = new StaffMenu(staff(2, 10));
    manager.openGUI(menu, player);
    server
        .getPluginManager()
        .registerEvents(
            new Listener() {
              @EventHandler
              public void onOpen(InventoryOpenEvent event) {
                event.setCancelled(true);
              }
            },
            plugin);

    menu.adminsOnly = true;
    menu.rebuild();

    assertFalse(manager.hasOpenGUI(player));
  }

  @Test
  void closedGUIOpensAgainAsANewSession() {
    StaffMenu menu = new StaffMenu(staff(2, 3));
    manager.openGUI(menu, player);
    player.closeInventory();
    menu.calls.clear();

    menu.adminsOnly = true;
    menu.rebuild();

    assertSame(menu, manager.getOpenGUI(player));
    assertSame(top(), menu.getInventory());
    assertEquals(2, headCount(top()));
    assertEquals(List.of("decorate", "onOpen"), menu.calls);
  }

  @Test
  void rebuildReplacesAnotherOpenGUI() {
    StaffMenu menu = new StaffMenu(staff(2, 3));
    manager.openGUI(menu, player);
    StaffMenu other = new StaffMenu(staff(1, 1));
    manager.openGUI(other, player);

    menu.rebuild();

    assertSame(menu, manager.getOpenGUI(player));
    assertTrue(other.calls.contains("onClose"));
  }

  @Test
  void rebuildDoesNothingWhenThePlayerIsOffline() {
    StaffMenu menu = new StaffMenu(staff(2, 3));
    manager.openGUI(menu, player);
    player.closeInventory();
    player.disconnect();
    menu.calls.clear();

    menu.rebuild();

    assertTrue(menu.calls.isEmpty());
    assertNull(manager.getOpenGUI(player));
  }

  @Test
  void rebuildBeforeOpeningThrows() {
    StaffMenu menu = new StaffMenu(staff(2, 3));

    assertThrows(IllegalStateException.class, menu::rebuild);
  }

  @Test
  void rebuildFromInsideDecorateThrows() {
    StaffMenu menu = new StaffMenu(staff(2, 3));
    manager.openGUI(menu, player);
    menu.rebuildInDecorate = true;

    assertThrows(IllegalStateException.class, menu::rebuild);
  }

  @Test
  void openingTheSameInstanceAgainStillThrows() {
    StaffMenu menu = new StaffMenu(staff(2, 3));
    manager.openGUI(menu, player);
    menu.rebuild();

    assertThrows(IllegalStateException.class, () -> manager.openGUI(menu, player));
    assertFalse(menu.calls.contains("onClose"));
  }
}
