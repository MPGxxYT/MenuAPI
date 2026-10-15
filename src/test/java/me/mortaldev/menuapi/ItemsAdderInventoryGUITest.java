package me.mortaldev.menuapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import java.util.ArrayList;
import java.util.List;
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
  void rebuildingAWrapperGUIShowsANewWrapper() {
    List<FakeWrapper> wrappers = new ArrayList<>();
    WrapperMenu menu =
        new WrapperMenu(null) {
          @Override
          protected Object createItemsAdderWrapper(Player player) {
            FakeWrapper wrapper = new FakeWrapper(true, false);
            wrappers.add(wrapper);
            return wrapper;
          }
        };
    manager.openGUI(menu, player);

    menu.rebuild();

    assertEquals(2, wrappers.size());
    assertSame(wrappers.get(1).inventory, player.getOpenInventory().getTopInventory());
    assertSame(wrappers.get(1).inventory, menu.getInventory());
    assertEquals(Material.EMERALD, wrappers.get(1).inventory.getItem(10).getType());
    assertSame(menu, manager.getOpenGUI(player));
    assertEquals(1, menu.opens);
  }

  @Test
  void rebuildingAWrappedInventoryRedrawsItsButtonsInPlace() {
    Inventory inventory = Bukkit.createInventory(null, 54, "Textured");
    int[] slot = {10};
    ItemsAdderInventoryGUI gui =
        new ItemsAdderInventoryGUI(inventory) {
          @Override
          public void decorate(Player player) {
            addButton(slot[0], new InventoryButton().creator(p -> new ItemStack(Material.EMERALD)));
          }
        };
    manager.openGUI(gui, player);

    slot[0] = 11;
    gui.rebuild();

    assertSame(inventory, player.getOpenInventory().getTopInventory());
    assertNull(inventory.getItem(10));
    assertEquals(Material.EMERALD, inventory.getItem(11).getType());
    assertSame(gui, manager.getOpenGUI(player));
  }

  @Test
  void closingEndsTheSession() {
    WrapperMenu menu = new WrapperMenu(new FakeWrapper(true, false));
    manager.openGUI(menu, player);

    player.closeInventory();

    assertFalse(manager.hasOpenGUI(player));
  }
}
