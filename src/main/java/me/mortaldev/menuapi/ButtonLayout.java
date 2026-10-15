package me.mortaldev.menuapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Which button covers which slots of a GUI, and drawing those buttons into an inventory. Each
 * button's icon is created once per draw and placed in every slot the button covers.
 */
final class ButtonLayout {

  private final Map<Integer, InventoryButton> buttonsBySlot = new HashMap<>();

  void place(InventoryButton button, int... slots) {
    for (int slot : slots) {
      if (slot < 0) {
        throw new IllegalArgumentException("Button slot cannot be negative, got: " + slot);
      }
      buttonsBySlot.put(slot, button);
    }
  }

  void place(IAButton button) {
    for (int slot : button.getOccupiedSlots()) {
      place(button, slot);
    }
  }

  InventoryButton buttonAt(int slot) {
    return buttonsBySlot.get(slot);
  }

  /**
   * Removes the button at a slot. An area button is removed from every slot it covers. The removed
   * slots are cleared in the inventory, if there is one.
   */
  void remove(int slot, Inventory inventory) {
    InventoryButton button = buttonsBySlot.get(slot);
    if (button == null) {
      return;
    }
    List<Integer> removed = button instanceof IAButton ? slotsOf(button) : List.of(slot);
    for (int removedSlot : removed) {
      buttonsBySlot.remove(removedSlot);
      if (inventory != null) {
        inventory.setItem(removedSlot, null);
      }
    }
  }

  /** Removes every button and clears its slots in the inventory, if there is one. */
  void clear(Inventory inventory) {
    if (inventory != null) {
      for (int slot : buttonsBySlot.keySet()) {
        if (slot < inventory.getSize()) {
          inventory.setItem(slot, null);
        }
      }
    }
    buttonsBySlot.clear();
  }

  /** Draws every button. */
  void render(Player player, Inventory inventory) {
    Map<InventoryButton, List<Integer>> slotsByButton = new LinkedHashMap<>();
    buttonsBySlot.forEach(
        (slot, button) -> slotsByButton.computeIfAbsent(button, b -> new ArrayList<>()).add(slot));
    checkSlotsFit(buttonsBySlot.keySet(), inventory);
    slotsByButton.forEach((button, slots) -> draw(player, inventory, button, slots));
  }

  /** Draws the button at a slot, in every slot that button covers. */
  void render(Player player, Inventory inventory, int slot) {
    InventoryButton button = buttonsBySlot.get(slot);
    if (button == null) {
      return;
    }
    List<Integer> slots = slotsOf(button);
    checkSlotsFit(slots, inventory);
    draw(player, inventory, button, slots);
  }

  private List<Integer> slotsOf(InventoryButton button) {
    List<Integer> slots = new ArrayList<>();
    buttonsBySlot.forEach(
        (slot, placed) -> {
          if (placed == button) {
            slots.add(slot);
          }
        });
    return slots;
  }

  private static void checkSlotsFit(Iterable<Integer> slots, Inventory inventory) {
    for (int slot : slots) {
      if (slot >= inventory.getSize()) {
        throw new IllegalArgumentException(
            "Button at slot " + slot + " is outside the inventory of size " + inventory.getSize());
      }
    }
  }

  private static void draw(
      Player player, Inventory inventory, InventoryButton button, List<Integer> slots) {
    ItemStack icon = button.getIconCreator() == null ? null : button.getIconCreator().apply(player);
    for (int slot : slots) {
      inventory.setItem(slot, icon == null ? null : icon.clone());
    }
  }
}
