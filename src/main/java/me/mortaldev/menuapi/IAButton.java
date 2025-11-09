package me.mortaldev.menuapi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * IAButton (Inventory Area Button) extends InventoryButton to support filling multiple slots
 * based on a rectangular size (width x height) anchored at a specific slot.
 *
 * The button fills slots in a grid pattern starting from the anchor slot.
 * For example, with anchor slot 0, width 3, and height 2, it fills slots:
 * 0, 1, 2 (first row) and 9, 10, 11 (second row, assuming 9-slot width inventory).
 */
public class IAButton extends InventoryButton {

    private int width = 1;
    private int height = 1;
    private int anchorSlot = 0;

    /**
     * Sets the size of the button area.
     *
     * @param width The width of the button area (max 9)
     * @param height The height of the button area (max 6)
     * @return This IAButton instance for chaining
     * @throws IllegalArgumentException if width > 9 or height > 6
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
     * Sets the anchor slot (top-left corner) from which the button area will be filled.
     * The anchor slot depends on the inventory size and the button size.
     *
     * @param slot The anchor slot index
     * @return This IAButton instance for chaining
     * @throws IllegalArgumentException if slot is negative
     */
    public IAButton anchor(int slot) {
        if (slot < 0) {
            throw new IllegalArgumentException("Anchor slot cannot be negative");
        }
        this.anchorSlot = slot;
        return this;
    }

    /**
     * Overrides the creator method to return IAButton for chaining.
     *
     * @param iconCreator The function to create the button's icon
     * @return This IAButton instance for chaining
     */
    @Override
    public IAButton creator(Function<Player, ItemStack> iconCreator) {
        super.creator(iconCreator);
        return this;
    }

    /**
     * Overrides the consumer method to return IAButton for chaining.
     *
     * @param eventConsumer The consumer to handle the click event
     * @return This IAButton instance for chaining
     */
    @Override
    public IAButton consumer(Consumer<InventoryClickEvent> eventConsumer) {
        super.consumer(eventConsumer);
        return this;
    }

    /**
     * Calculates all slot indices that this button should occupy based on
     * the anchor slot, width, and height.
     *
     * Assumes standard Minecraft inventory layout where each row has 9 slots.
     *
     * @return List of all slot indices this button occupies
     */
    public List<Integer> getOccupiedSlots() {
        List<Integer> slots = new ArrayList<>();

        // Calculate the starting row and column from the anchor slot
        int anchorRow = anchorSlot / 9;
        int anchorCol = anchorSlot % 9;

        // Fill slots in a rectangular pattern
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int currentRow = anchorRow + row;
                int currentCol = anchorCol + col;

                // Ensure we don't overflow the inventory width (9 slots per row)
                if (currentCol >= 9) {
                    continue;
                }

                int slotIndex = (currentRow * 9) + currentCol;
                slots.add(slotIndex);
            }
        }

        return slots;
    }

    /**
     * Gets the width of this button area.
     *
     * @return The width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the height of this button area.
     *
     * @return The height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the anchor slot of this button area.
     *
     * @return The anchor slot index
     */
    public int getAnchorSlot() {
        return anchorSlot;
    }
}