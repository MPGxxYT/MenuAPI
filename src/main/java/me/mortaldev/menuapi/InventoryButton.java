package me.mortaldev.menuapi;

import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A clickable icon in an {@link InventoryGUI}: a function that creates the icon, and a handler that
 * runs when the icon is clicked. Both are optional and set with method chaining:
 *
 * <pre>{@code
 * addButton(13, new InventoryButton()
 *     .creator(player -> new ItemStack(Material.DIAMOND))
 *     .consumer(event -> event.getWhoClicked().sendMessage("Clicked!")));
 * }</pre>
 *
 * <p>A button does nothing until it is added to a GUI with {@link InventoryGUI#addButton(int,
 * InventoryButton)}, normally inside {@link InventoryGUI#decorate(Player)}. The same instance can be
 * placed in several slots; its icon is then created once per draw and copied into each slot.
 *
 * @see IAButton for a button that covers a rectangle of slots
 */
public class InventoryButton {

  private Function<Player, ItemStack> iconCreator;
  private Consumer<InventoryClickEvent> eventConsumer;

  /**
   * Creates a button with no icon and no click handler. Set them with {@link #creator} and {@link
   * #consumer}.
   */
  public InventoryButton() {}

  /**
   * Sets the function that creates this button's icon.
   *
   * <p>The function runs every time the button is drawn: when the GUI opens, on every {@link
   * InventoryGUI#rebuild()}, and on every {@link InventoryGUI#refreshButtons()} or {@link
   * InventoryGUI#refreshButton(int)}. It receives the player the GUI is open for, so the icon can
   * differ per player. Values read inside the function are read again on each draw, so a refresh
   * shows their current state. Keep it cheap, since it may run every few ticks with auto-refresh.
   *
   * <p>The returned item is cloned into each slot, so changing it later does not change what is on
   * screen. Returning {@code null}, or never setting a creator, leaves the slot empty while the
   * button still responds to clicks there.
   *
   * <p>Calling this again replaces the previous function. It runs on the server's main thread.
   *
   * @param iconCreator Creates the icon for the given player. May be {@code null} for an empty slot.
   * @return This button, for chaining.
   */
  public InventoryButton creator(Function<Player, ItemStack> iconCreator) {
    this.iconCreator = iconCreator;
    return this;
  }

  /**
   * Sets the handler that runs when a player clicks this button.
   *
   * <p>The handler receives the Bukkit {@link InventoryClickEvent}. Use {@code event.getWhoClicked()}
   * for the player (cast it to {@link Player}), and {@code event.getClick()} to tell left, right and
   * shift clicks apart. It runs only for clicks in the GUI's own (top) inventory, after {@link
   * InventoryGUI#onClick(InventoryClickEvent)}, on the server's main thread.
   *
   * <p>By default the event arrives already cancelled, so the player cannot take the icon. You don't
   * need to call {@code event.setCancelled(true)}. If the GUI allows top-inventory clicks with
   * {@link InventoryGUI#allowTopInventoryClick(boolean)}, the event arrives uncancelled and the
   * handler can cancel it itself.
   *
   * <p>The handler may open another GUI, call {@link InventoryGUI#rebuild()}, or close the inventory.
   * Calling this again replaces the previous handler. Without a handler, clicks on the button do
   * nothing beyond the GUI's click rules.
   *
   * @param eventConsumer Runs on each click. May be {@code null} for a button that only shows an
   *     icon.
   * @return This button, for chaining.
   */
  public InventoryButton consumer(Consumer<InventoryClickEvent> eventConsumer) {
    this.eventConsumer = eventConsumer;
    return this;
  }

  /**
   * Returns the click handler set with {@link #consumer}. MenuAPI calls it for you; you only need
   * this to inspect or reuse a button's handler.
   *
   * @return The click handler, or {@code null} if none was set.
   */
  public Consumer<InventoryClickEvent> getEventConsumer() {
    return this.eventConsumer;
  }

  /**
   * Returns the icon function set with {@link #creator}. MenuAPI calls it for you when drawing;
   * you only need this to inspect or reuse a button's icon.
   *
   * @return The icon function, or {@code null} if none was set.
   */
  public Function<Player, ItemStack> getIconCreator() {
    return this.iconCreator;
  }
}
