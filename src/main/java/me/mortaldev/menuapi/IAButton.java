package me.mortaldev.menuapi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * An area button (Inventory Area Button): one button that covers a rectangle of slots. Every slot in
 * the rectangle shows the same icon and runs the same click handler.
 *
 * <p>The rectangle starts at the {@link #anchor(int) anchor} slot, its top-left corner, and extends
 * {@link #size(int, int) width} slots to the right and height rows down. Rows are 9 slots wide, so
 * anchor 0 with size 3 by 2 covers slots 0, 1, 2 and 9, 10, 11:
 *
 * <pre>{@code
 * addButton(new IAButton()
 *     .size(3, 2)
 *     .anchor(0)
 *     .creator(player -> new ItemStack(Material.DIAMOND_SWORD))
 *     .consumer(event -> startGame((Player) event.getWhoClicked())));
 * }</pre>
 *
 * <p>Set the size and anchor before passing the button to {@link InventoryGUI#addButton(IAButton)}.
 * The covered slots are worked out when the button is added, so changing the size or anchor
 * afterwards does not move it. A 1 by 1 area button at anchor 0 is the default.
 *
 * <p>{@link InventoryGUI#getButton(int)} returns this button for any slot it covers, and {@link
 * InventoryGUI#removeButton(int)} or {@link InventoryGUI#refreshButton(int)} on any covered slot
 * affects the whole area.
 */
public class IAButton extends InventoryButton {

  private int width = 1;
  private int height = 1;
  private int anchorSlot = 0;

  /**
   * Creates a 1 by 1 area button anchored at slot 0, with no icon and no click handler. Set them
   * with {@link #size}, {@link #anchor}, {@link #creator} and {@link #consumer}.
   */
  public IAButton() {}

  /**
   * Sets how many slots wide and how many rows tall the area is.
   *
   * <p>The area must also fit the inventory it is added to: running past the right edge makes {@link
   * InventoryGUI#addButton(IAButton)} throw, and running past the last row makes drawing the GUI
   * throw. Call this before adding the button to a GUI.
   *
   * @param width Slots per row, from 1 to 9.
   * @param height Number of rows, from 1 to 6.
   * @return This button, for chaining.
   * @throws IllegalArgumentException If width is not between 1 and 9, or height is not between 1 and
   *     6.
   */
  public IAButton size(int width, int height) {
    if (width > 9) {
      throw new IllegalArgumentException("Width cannot exceed 9 (max inventory width)");
    }
    if (height > 6) {
      throw new IllegalArgumentException("Height cannot exceed 6 (max inventory height)");
    }
    if (width < 1 || height < 1) {
      throw new IllegalArgumentException("Width and height must be at least 1");
    }
    this.width = width;
    this.height = height;
    return this;
  }

  /**
   * Sets the area's top-left slot. Slots count from 0 in the top-left corner of the inventory, 9 per
   * row, so slot 10 is the second slot of the second row. Call this before adding the button to a
   * GUI.
   *
   * @param slot The top-left slot of the area, 0 or higher.
   * @return This button, for chaining.
   * @throws IllegalArgumentException If the slot is negative.
   */
  public IAButton anchor(int slot) {
    if (slot < 0) {
      throw new IllegalArgumentException("Anchor slot cannot be negative");
    }
    this.anchorSlot = slot;
    return this;
  }

  /**
   * Sets the function that creates the icon shown in every slot of the area. Behaves like {@link
   * InventoryButton#creator(Function)}, and returns this {@code IAButton} so {@link #size} and
   * {@link #anchor} can still be chained after it.
   *
   * @param iconCreator Creates the icon for the given player. May be {@code null} for empty slots.
   * @return This button, for chaining.
   */
  @Override
  public IAButton creator(Function<Player, ItemStack> iconCreator) {
    super.creator(iconCreator);
    return this;
  }

  /**
   * Sets the handler that runs when a player clicks any slot of the area. Behaves like {@link
   * InventoryButton#consumer(Consumer)}; {@code event.getSlot()} tells you which slot was clicked.
   * Returns this {@code IAButton} so {@link #size} and {@link #anchor} can still be chained after it.
   *
   * @param eventConsumer Runs on each click. May be {@code null} for an area that only shows icons.
   * @return This button, for chaining.
   */
  @Override
  public IAButton consumer(Consumer<InventoryClickEvent> eventConsumer) {
    super.consumer(eventConsumer);
    return this;
  }

  /**
   * Lists every slot the area covers, row by row from the anchor. MenuAPI calls this when the button
   * is added to a GUI; you can call it to check a layout. It does not check the bottom edge, since
   * the inventory's height is not known here.
   *
   * @return The covered slot indices, left to right and top to bottom.
   * @throws IllegalArgumentException If the area runs past the right edge of a 9-slot row.
   */
  public List<Integer> getOccupiedSlots() {
    List<Integer> slots = new ArrayList<>();

    // Calculate the starting row and column from the anchor slot
    int anchorRow = anchorSlot / 9;
    int anchorCol = anchorSlot % 9;
    if (anchorCol + width > 9) {
      throw new IllegalArgumentException(
          "Area button at anchor "
              + anchorSlot
              + " with width "
              + width
              + " runs past the right edge of the inventory");
    }

    // Fill slots in a rectangular pattern
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        slots.add((anchorRow + row) * 9 + anchorCol + col);
      }
    }

    return slots;
  }

  /**
   * Returns the area's width, set with {@link #size}.
   *
   * @return Slots per row, from 1 to 9. Defaults to 1.
   */
  public int getWidth() {
    return width;
  }

  /**
   * Returns the area's height, set with {@link #size}.
   *
   * @return Number of rows, from 1 to 6. Defaults to 1.
   */
  public int getHeight() {
    return height;
  }

  /**
   * Returns the area's top-left slot, set with {@link #anchor}.
   *
   * @return The anchor slot. Defaults to 0.
   */
  public int getAnchorSlot() {
    return anchorSlot;
  }
}
