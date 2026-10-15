package me.mortaldev.menuapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class GUIManagerTest {

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

  /** A GUI whose hooks record what happened, and which never calls super. */
  private static class TestMenu extends InventoryGUI {
    final List<String> calls = new ArrayList<>();
    int refreshes = 0;

    @Override
    protected Inventory createInventory() {
      return Bukkit.createInventory(null, 27, "Test");
    }

    @Override
    public void decorate(Player player) {
      calls.add("decorate");
      addButton(
          13,
          new InventoryButton()
              .creator(p -> new ItemStack(Material.DIAMOND))
              .consumer(e -> calls.add("button")));
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
      calls.add("onOpen");
    }

    @Override
    public void onClick(InventoryClickEvent event) {
      calls.add("onClick");
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

  private static class OtherMenu extends TestMenu {}

  private InventoryClickEvent click(int rawSlot) {
    InventoryClickEvent event =
        new InventoryClickEvent(
            player.getOpenInventory(),
            SlotType.CONTAINER,
            rawSlot,
            ClickType.LEFT,
            InventoryAction.PICKUP_ALL);
    server.getPluginManager().callEvent(event);
    return event;
  }

  @Test
  void openGUIDecoratesDrawsButtonsAndTracksSessionWithoutSuperCalls() {
    TestMenu menu = new TestMenu();
    manager.openGUI(menu, player);

    assertEquals(List.of("decorate", "onOpen"), menu.calls);
    assertEquals(Material.DIAMOND, menu.getInventory().getItem(13).getType());
    assertSame(menu, manager.getOpenGUI(player));
    assertTrue(manager.hasOpenGUI(player));
  }

  @Test
  void openingTheSameInstanceTwiceThrows() {
    TestMenu menu = new TestMenu();
    manager.openGUI(menu, player);
    player.closeInventory();

    assertThrows(IllegalStateException.class, () -> manager.openGUI(menu, player));
  }

  @Test
  void topClickIsCancelledByDefaultAndRunsHookBeforeButton() {
    TestMenu menu = new TestMenu();
    manager.openGUI(menu, player);
    menu.calls.clear();

    InventoryClickEvent event = click(13);

    assertTrue(event.isCancelled());
    assertEquals(List.of("onClick", "button"), menu.calls);
  }

  @Test
  void allowTopInventoryClickLeavesClickUncancelled() {
    TestMenu menu = new TestMenu();
    menu.allowTopInventoryClick(true);
    manager.openGUI(menu, player);

    assertFalse(click(13).isCancelled());
  }

  @Test
  void bottomClickIsCancelledByDefaultAndDoesNotRunButtons() {
    TestMenu menu = new TestMenu();
    manager.openGUI(menu, player);
    menu.calls.clear();

    // 27-slot top inventory, so raw slot 27 + 13 is in the player's inventory
    InventoryClickEvent event = click(27 + 13);

    assertTrue(event.isCancelled());
    assertEquals(List.of("onClick"), menu.calls);
  }

  @Test
  void closeCallsHookAndEndsSession() {
    TestMenu menu = new TestMenu();
    manager.openGUI(menu, player);

    player.closeInventory();

    assertTrue(menu.calls.contains("onClose"));
    assertFalse(manager.hasOpenGUI(player));
    assertNull(manager.getOpenGUI(player));
  }

  @Test
  void autoRefreshRunsEveryIntervalAndStopsOnClose() {
    TestMenu menu = new TestMenu();
    menu.enableAutoRefresh(plugin, 10L);
    manager.openGUI(menu, player);

    server.getScheduler().performTicks(30L);
    assertEquals(3, menu.refreshes);

    player.closeInventory();
    server.getScheduler().performTicks(30L);
    assertEquals(3, menu.refreshes);
  }

  @Test
  void openingAnotherGUIStopsThePreviousRefreshTask() {
    TestMenu first = new TestMenu();
    first.enableAutoRefresh(plugin, 10L);
    manager.openGUI(first, player);
    server.getScheduler().performTicks(10L);

    manager.openGUI(new TestMenu(), player);
    server.getScheduler().performTicks(30L);

    assertEquals(1, first.refreshes);
  }

  @Test
  void disableAutoRefreshStopsARunningTask() {
    TestMenu menu = new TestMenu();
    menu.enableAutoRefresh(plugin, 10L);
    manager.openGUI(menu, player);
    server.getScheduler().performTicks(10L);

    menu.disableAutoRefresh();
    server.getScheduler().performTicks(30L);

    assertEquals(1, menu.refreshes);
  }

  @Test
  void refreshAllGUIsOfTypeOnlyRefreshesMatchingGUIs() {
    PlayerMock other = server.addPlayer();
    TestMenu plain = new TestMenu();
    OtherMenu special = new OtherMenu();
    manager.openGUI(plain, player);
    manager.openGUI(special, other);

    manager.refreshAllGUIsOfType(OtherMenu.class);

    assertEquals(0, plain.refreshes);
    assertEquals(1, special.refreshes);
  }
}
