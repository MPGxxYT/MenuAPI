package me.mortaldev.menuapi;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class InventoryGUI implements InventoryHandler {

  private final Map<Integer, InventoryButton> buttonMap = new HashMap<>();
  private boolean allowBottomInventoryClick;
  private boolean allowTopInventoryClick;
  private Inventory inventory;

  public InventoryGUI() {
    allowBottomInventoryClick = false;
    allowTopInventoryClick = false;
  }

  public Inventory getInventory() {
    if (inventory == null) {
      inventory = createInventory();
    }
    return inventory;
  }

  public void allowBottomInventoryClick(boolean b) {
    this.allowBottomInventoryClick = b;
  }

  public void allowTopInventoryClick(boolean b) {
    this.allowTopInventoryClick = b;
  }

  public void addButton(int slot, InventoryButton button) {
    this.buttonMap.put(slot, button);
  }

  public void decorate(Player player) {
    this.buttonMap.forEach(
        (slot, button) -> {
          ItemStack icon = button.getIconCreator().apply(player);
          this.inventory.setItem(slot, icon);
        });
  }

  @Override
  public void onClick(InventoryClickEvent event) {

    if (event.getView().getBottomInventory() == event.getClickedInventory()
        && !allowBottomInventoryClick) {
      event.setCancelled(true);
    } else if (event.getView().getTopInventory() == event.getClickedInventory()
        && !allowTopInventoryClick) {
      event.setCancelled(true);
    }
    if (event.getView().getTopInventory() == event.getClickedInventory()) {
      int slot = event.getSlot();
      InventoryButton button = this.buttonMap.get(slot);
      if (button != null) {
        button.getEventConsumer().accept(event);
      }
    }
  }

  @Override
  public void onOpen(InventoryOpenEvent event) {
    this.decorate((Player) event.getPlayer());
  }

  @Override
  public void onClose(InventoryCloseEvent event) {
  }

  protected abstract Inventory createInventory();
}
