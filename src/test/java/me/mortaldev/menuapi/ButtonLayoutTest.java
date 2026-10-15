package me.mortaldev.menuapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.seeseemelk.mockbukkit.MockBukkit;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ButtonLayoutTest {

  private ButtonLayout layout;
  private Inventory inventory;
  private AtomicInteger creatorCalls;

  @BeforeEach
  void setUp() {
    MockBukkit.mock();
    layout = new ButtonLayout();
    inventory = Bukkit.createInventory(null, 27, "Test");
    creatorCalls = new AtomicInteger();
  }

  @AfterEach
  void tearDown() {
    MockBukkit.unmock();
  }

  private InventoryButton countingButton(Material material) {
    return new InventoryButton()
        .creator(
            p -> {
              creatorCalls.incrementAndGet();
              return new ItemStack(material);
            });
  }

  private IAButton areaButton() {
    IAButton button = new IAButton().size(3, 2).anchor(0);
    button.creator(
        p -> {
          creatorCalls.incrementAndGet();
          return new ItemStack(Material.DIAMOND);
        });
    return button;
  }

  @Test
  void areaButtonIconIsCreatedOnceAndDrawnInEverySlot() {
    layout.place(areaButton());

    layout.render(null, inventory);

    assertEquals(1, creatorCalls.get());
    for (int slot : new int[] {0, 1, 2, 9, 10, 11}) {
      assertEquals(Material.DIAMOND, inventory.getItem(slot).getType());
    }
  }

  @Test
  void multiSlotButtonIconIsCreatedOnce() {
    layout.place(countingButton(Material.EMERALD), 3, 4, 5);

    layout.render(null, inventory);

    assertEquals(1, creatorCalls.get());
    assertEquals(Material.EMERALD, inventory.getItem(5).getType());
  }

  @Test
  void slotOutsideInventoryFailsBeforeDrawingAnything() {
    layout.place(countingButton(Material.EMERALD), 0);
    layout.place(countingButton(Material.STONE), 30);

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> layout.render(null, inventory));

    assertTrue(e.getMessage().contains("30"));
    assertNull(inventory.getItem(0));
  }

  @Test
  void negativeSlotIsRejectedAtPlacement() {
    assertThrows(
        IllegalArgumentException.class, () -> layout.place(countingButton(Material.STONE), -1));
  }

  @Test
  void renderingOneSlotOfAnAreaButtonDrawsTheWholeArea() {
    layout.place(areaButton());

    layout.render(null, inventory, 10);

    assertEquals(Material.DIAMOND, inventory.getItem(0).getType());
    assertEquals(Material.DIAMOND, inventory.getItem(11).getType());
  }

  @Test
  void removingAnAreaButtonClearsEverySlotItCovers() {
    layout.place(areaButton());
    layout.render(null, inventory);

    layout.remove(1, inventory);

    for (int slot : new int[] {0, 1, 2, 9, 10, 11}) {
      assertNull(layout.buttonAt(slot));
      assertNull(inventory.getItem(slot));
    }
  }

  @Test
  void removingOneSlotOfAMultiSlotButtonKeepsTheOthers() {
    InventoryButton button = countingButton(Material.EMERALD);
    layout.place(button, 3, 4);
    layout.render(null, inventory);

    layout.remove(3, inventory);

    assertNull(inventory.getItem(3));
    assertSame(button, layout.buttonAt(4));
  }

  @Test
  void areaButtonPastTheRightEdgeIsRejected() {
    IAButton button = new IAButton().size(3, 1).anchor(7);

    assertThrows(IllegalArgumentException.class, button::getOccupiedSlots);
  }
}
