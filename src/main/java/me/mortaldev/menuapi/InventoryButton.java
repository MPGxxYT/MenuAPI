package me.mortaldev.menuapi;

import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a button within a custom inventory GUI.
 * It defines how the button's icon is created and what action it performs when clicked.
 */
public class InventoryButton {

    private Function<Player, ItemStack> iconCreator; //
    private Consumer<InventoryClickEvent> eventConsumer; //

    /**
     * Sets the function responsible for creating the button's icon.
     * The function takes a {@link Player} as input and returns an {@link ItemStack}.
     *
     * @param iconCreator The function to create the button's icon.
     * @return This InventoryButton instance for chaining.
     */
    public InventoryButton creator(Function<Player, ItemStack> iconCreator) {
        this.iconCreator = iconCreator; //
        return this; //
    }

    /**
     * Sets the consumer that defines the action to be performed when the button is clicked.
     * The consumer takes an {@link InventoryClickEvent} as input.
     *
     * @param eventConsumer The consumer to handle the click event.
     * @return This InventoryButton instance for chaining.
     */
    public InventoryButton consumer(Consumer<InventoryClickEvent> eventConsumer) {
        this.eventConsumer = eventConsumer; //
        return this; //
    }

    /**
     * Returns the consumer that handles the click event for this button.
     *
     * @return The consumer for the click event.
     */
    public Consumer<InventoryClickEvent> getEventConsumer() {
        return this.eventConsumer; //
    }

    /**
     * Returns the function that creates the icon for this button.
     *
     * @return The function to create the icon.
     */
    public Function<Player, ItemStack> getIconCreator() {
        return this.iconCreator; //
    }
}