package me.mortaldev.menuapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemsAdderInventoryGUITest {

  private ServerMock server;
  private GUIManager manager;
  private PlayerMock player;

  @BeforeEach
  void setUp() {
    server = MockBukkit.mock();
    Plugin plugin = MockBukkit.createMockPlugin();
    manager = new GUIManager();
    server.getPluginManager().registerEvents(new GUIListener(manager), plugin);
    player = server.addPlayer();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  /** Stands in for ItemsAdder's TexturedInventoryWrapper, which is found by reflection. */
  static class FakeWrapper {
    final Inventory inventory = Bukkit.createInventory(null, 54, "Textured");
    final boolean opens;
    final boolean throwsAfter;

    FakeWrapper(boolean opens, boolean throwsAfter) {
      this.opens = opens;
      this.throwsAfter = throwsAfter;
    }

    public void showInventory(Player player) {
      if (opens) {
        player.openInventory(inventory);
      }
      if (throwsAfter) {
        throw new RuntimeException("ItemsAdder internal error");
      }
    }
  }

  static class WrapperMenu extends ItemsAdderInventoryGUI {
    final Object wrapper;
    int opens = 0;

    WrapperMenu(Object wrapper) {
      this.wrapper = wrapper;
    }

    @Override
    protected Object createItemsAdderWrapper(Player player) {
      return wrapper;
    }

    @Override
    public void decorate(Player player) {
      addButton(10, new InventoryButton().creator(p -> new ItemStack(Material.EMERALD)));
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
      opens++;
    }
  }

  @Test
  void wrapperOpensAndSessionStarts() {
    FakeWrapper wrapper = new FakeWrapper(true, false);
    WrapperMenu menu = new WrapperMenu(wrapper);

    manager.openGUI(menu, player);

    assertSame(menu, manager.getOpenGUI(player));
    assertSame(wrapper.inventory, menu.getInventory());
    assertEquals(Material.EMERALD, wrapper.inventory.getItem(10).getType());
    assertEquals(1, menu.opens);
  }

  @Test
  void wrapperThatThrowsAfterOpeningStillStartsSession() {
    WrapperMenu menu = new WrapperMenu(new FakeWrapper(true, true));

    manager.openGUI(menu, player);

    assertSame(menu, manager.getOpenGUI(player));
  }

  @Test
  void wrapperThatThrowsWithoutOpeningFailsAndStartsNoSession() {
    WrapperMenu menu = new WrapperMenu(new FakeWrapper(false, true));

    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> manager.openGUI(menu, player));

    assertEquals("ItemsAdder internal error", e.getCause().getMessage());
    assertFalse(manager.hasOpenGUI(player));
  }

  @Test
  void wrapperThatOpensNothingStartsNoSession() {
    WrapperMenu menu = new WrapperMenu(new FakeWrapper(false, false));

    manager.openGUI(menu, player);

    assertFalse(manager.hasOpenGUI(player));
  }

  @Test
  void wrapperWithoutShowInventoryFails() {
    WrapperMenu menu = new WrapperMenu(new Object());

    assertThrows(IllegalStateException.class, () -> manager.openGUI(menu, player));
    assertFalse(manager.hasOpenGUI(player));
  }

  @Test
  void existingInventoryAlreadyOnScreenIsNotReopened() {
    Inventory inventory = Bukkit.createInventory(null, 54, "Textured");
    player.openInventory(inventory);
    ItemsAdderInventoryGUI gui = new ItemsAdderInventoryGUI(inventory);
    gui.addButton(10, new InventoryButton().creator(p -> new ItemStack(Material.EMERALD)));

    manager.openGUI(gui, player);

    assertSame(gui, manager.getOpenGUI(player));
    assertEquals(Material.EMERALD, inventory.getItem(10).getType());
    assertSame(inventory, player.getOpenInventory().getTopInventory());
  }

  @Test
  void closingEndsTheSession() {
    WrapperMenu menu = new WrapperMenu(new FakeWrapper(true, false));
    manager.openGUI(menu, player);

    player.closeInventory();

    assertFalse(manager.hasOpenGUI(player));
  }
}
