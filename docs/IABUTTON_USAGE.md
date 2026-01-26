# IAButton Usage Guide

`IAButton` is an extension of `InventoryButton` that allows you to fill multiple slots in a rectangular pattern. Perfect for creating large, clickable areas in your GUIs.

## Table of Contents
- [Features](#features)
- [Basic Usage](#basic-usage)
- [Slot Calculation](#slot-calculation)
- [Refresh Behavior](#refresh-behavior)
- [Advanced Examples](#advanced-examples)
- [Best Practices](#best-practices)
- [Comparison with InventoryButton](#comparison-with-inventorybutton)

---

## Features

- **Size Control**: Define width (max 9) and height (max 6) for the button area
- **Anchor Positioning**: Set the top-left corner slot from which the button area extends
- **Auto-fill**: Automatically fills all slots in the defined area with the same item and click handler
- **Smart Refresh**: Refreshing any slot in the area refreshes the entire button
- **Method Chaining**: Clean, fluent API for configuration
- **Validation**: Automatic validation of size and anchor constraints

---

## Basic Usage

### Simple Example

```java
// Create a 3x2 button starting at slot 0
IAButton playButton = new IAButton()
    .size(3, 2)           // 3 wide, 2 tall
    .anchor(0)            // Start at slot 0 (top-left)
    .creator(player -> {
        // Create the item to display in all occupied slots
        return new ItemStack(Material.DIAMOND_SWORD);
    })
    .consumer(event -> {
        // Handle clicks on any of the occupied slots
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        player.sendMessage("Play button clicked!");
    });

// Add to your GUI (automatically fills slots 0,1,2,9,10,11)
addButton(playButton);
```

### In a Complete GUI

```java
public class PlayMenu extends InventoryGUI {

  @Override
  protected Inventory createInventory() {
    return Bukkit.createInventory(null, 54, "Play Menu");
  }

  @Override
  public void decorate(Player player) {
    // Add three large buttons across the top
    addButton(createGamemodeButton(Gamemode.CTF, 0));
    addButton(createGamemodeButton(Gamemode.TDM, 3));
    addButton(createGamemodeButton(Gamemode.KOTH, 6));

    super.decorate(player);
  }

  private IAButton createGamemodeButton(Gamemode mode, int anchorSlot) {
    return new IAButton()
        .size(3, 6)  // 3 wide, full height
        .anchor(anchorSlot)
        .creator(player -> {
          ItemStack item = new ItemStack(mode.getMaterial());
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§3§l" + mode.getDisplayName());
          meta.setLore(Arrays.asList(
              mode.getDescription(),
              "",
              "§7( click to play )"
          ));
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> {
          event.setCancelled(true);
          Player player = (Player) event.getWhoClicked();
          LobbyManager.getInstance().joinLobby(player, mode);
          player.closeInventory();
        });
  }
}
```

---

## Slot Calculation

The `IAButton` calculates which slots to fill based on:
- **Anchor slot**: The starting position (top-left corner)
- **Width**: Number of columns to fill (horizontally)
- **Height**: Number of rows to fill (vertically)

### How Slots Are Calculated

Minecraft inventories are 9 slots wide. Slots are numbered left-to-right, top-to-bottom:
```
 0  1  2  3  4  5  6  7  8
 9 10 11 12 13 14 15 16 17
18 19 20 21 22 23 24 25 26
27 28 29 30 31 32 33 34 35
36 37 38 39 40 41 42 43 44
45 46 47 48 49 50 51 52 53
```

### Example 1: `anchor(0)` and `size(3, 2)`
Fills a 3×2 area starting at slot 0:
```
[X][X][X] 3  4  5  6  7  8
[X][X][X]12 13 14 15 16 17
```
**Occupied slots**: 0, 1, 2, 9, 10, 11

### Example 2: `anchor(10)` and `size(2, 3)`
Fills a 2×3 area starting at slot 10:
```
 0  1  2  3  4  5  6  7  8
 9 [X][X]12 13 14 15 16 17
18 [X][X]21 22 23 24 25 26
27 [X][X]30 31 32 33 34 35
```
**Occupied slots**: 10, 11, 19, 20, 28, 29

### Example 3: Three Full-Height Buttons `size(3, 6)`
```
CTF         TDM         KOTH
[X][X][X]  [X][X][X]  [X][X][X]
[X][X][X]  [X][X][X]  [X][X][X]
[X][X][X]  [X][X][X]  [X][X][X]
[X][X][X]  [X][X][X]  [X][X][X]
[X][X][X]  [X][X][X]  [X][X][X]
[X][X][X]  [X][X][X]  [X][X][X]
```
- CTF: `anchor(0)`, slots 0-2, 9-11, 18-20, 27-29, 36-38, 45-47
- TDM: `anchor(3)`, slots 3-5, 12-14, 21-23, 30-32, 39-41, 48-50
- KOTH: `anchor(6)`, slots 6-8, 15-17, 24-26, 33-35, 42-44, 51-53

---

## Refresh Behavior

IAButton has special refresh behavior that differs from regular InventoryButton.

### Smart Refresh

When you refresh **any slot** occupied by an IAButton, **all slots** in that button refresh:

```java
// Create a 2x2 IAButton at slot 0 (occupies 0, 1, 9, 10)
IAButton button = new IAButton()
    .size(2, 2)
    .anchor(0)
    .creator(player -> dynamicItem())
    .consumer(event -> event.setCancelled(true));

addButton(button);

// Later, refresh any slot in the button
refreshButton(0);   // Refreshes slots 0, 1, 9, 10
refreshButton(1);   // Also refreshes 0, 1, 9, 10
refreshButton(9);   // Also refreshes 0, 1, 9, 10
refreshButton(10);  // Also refreshes 0, 1, 9, 10
```

### Why This Matters

This ensures the button area always displays consistently, even if you only refresh one slot.

```java
public class DynamicPlayMenu extends InventoryGUI {

  public DynamicPlayMenu() {
    super();
    enableAutoRefresh(plugin, 40L); // Auto-refresh every 2 seconds
  }

  @Override
  public void decorate(Player player) {
    // Create a large dynamic button
    IAButton statusButton = new IAButton()
        .size(3, 2)
        .anchor(0)
        .creator(p -> {
          // This data changes every refresh
          int playerCount = getPlayerCount();
          ItemStack item = new ItemStack(Material.PLAYER_HEAD);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§e" + playerCount + " Players Online");
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> event.setCancelled(true));

    addButton(statusButton);
    super.decorate(player);
  }

  // When auto-refresh triggers, ALL slots (0,1,2,9,10,11) update together
  @Override
  protected void refreshGUI() {
    refreshButtons(); // IAButton slots refresh as a group
  }
}
```

---

## Advanced Examples

### Example 1: ItemsAdder Custom Textures

Use IAButton with ItemsAdder for large custom texture areas:

```java
public class CustomPlayMenu extends ItemsAdderInventoryGUI {

  @Override
  protected Object createItemsAdderWrapper(Player player) {
    FontImageWrapper fontImageWrapper = new FontImageWrapper("crusaders:play");
    return new TexturedInventoryWrapper(
        player, 54, "", 32, -48, fontImageWrapper
    );
  }

  @Override
  public void decorate(Player player) {
    // Three full-height gamemode buttons
    for (Gamemode gamemode : Gamemode.values()) {
      addButton(createGamemodeButton(gamemode));
    }
    super.decorate(player);
  }

  private IAButton createGamemodeButton(Gamemode gamemode) {
    int anchor = switch (gamemode) {
      case CTF -> 0;
      case TDM -> 3;
      case KOTH -> 6;
    };

    return new IAButton()
        .size(3, 6)
        .anchor(anchor)
        .creator(player -> {
          // Use ItemsAdder custom item
          CustomStack custom = CustomStack.getInstance("crusaders:empty");
          ItemStack item = custom.getItemStack();
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§3§l" + gamemode.getDisplayName());
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> {
          event.setCancelled(true);
          Player p = (Player) event.getWhoClicked();
          joinGamemode(p, gamemode);
        });
  }
}
```

### Example 2: Dynamic Grid Layout

Create a grid of buttons with dynamic sizing:

```java
public class GridMenu extends InventoryGUI {

  @Override
  protected Inventory createInventory() {
    return Bukkit.createInventory(null, 54, "Grid Menu");
  }

  @Override
  public void decorate(Player player) {
    // Create a 3x3 grid of 2x2 buttons
    int buttonSize = 2;
    int spacing = 1;

    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        int anchorSlot = (row * (buttonSize + spacing) * 9)
                       + (col * (buttonSize + spacing));

        IAButton button = new IAButton()
            .size(buttonSize, buttonSize)
            .anchor(anchorSlot)
            .creator(p -> createGridItem(row, col))
            .consumer(event -> handleGridClick(row, col, event));

        addButton(button);
      }
    }

    super.decorate(player);
  }

  private ItemStack createGridItem(int row, int col) {
    ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName("§eButton " + row + "," + col);
    item.setItemMeta(meta);
    return item;
  }

  private void handleGridClick(int row, int col, InventoryClickEvent event) {
    event.setCancelled(true);
    Player player = (Player) event.getWhoClicked();
    player.sendMessage("§aClicked grid position: " + row + "," + col);
  }
}
```

### Example 3: Pagination with Large Buttons

```java
public class PaginatedIAMenu extends InventoryGUI {

  private final List<GameMode> modes;
  private int page = 0;

  public PaginatedIAMenu(List<GameMode> modes) {
    this.modes = modes;
  }

  @Override
  protected Inventory createInventory() {
    return Bukkit.createInventory(null, 54, "Game Modes - Page " + (page + 1));
  }

  @Override
  public void decorate(Player player) {
    // 3 large buttons per page (3 wide, 4 tall each)
    int startIndex = page * 3;
    int endIndex = Math.min(startIndex + 3, modes.size());

    for (int i = startIndex; i < endIndex; i++) {
      GameMode mode = modes.get(i);
      int col = (i - startIndex) * 3;

      IAButton button = new IAButton()
          .size(3, 4)
          .anchor(col)
          .creator(p -> createModeItem(mode))
          .consumer(event -> handleModeClick(mode, event));

      addButton(button);
    }

    // Navigation buttons
    if (page > 0) {
      addButton(45, previousButton());
    }
    if (endIndex < modes.size()) {
      addButton(53, nextButton());
    }

    super.decorate(player);
  }
}
```

---

## Best Practices

### 1. Always Specify Size

```java
// Bad - defaults to 1x1 (pointless IAButton)
IAButton button = new IAButton()
    .anchor(0)
    .creator(player -> item)
    .consumer(event -> action);

// Good - specify size
IAButton button = new IAButton()
    .size(3, 2)
    .anchor(0)
    .creator(player -> item)
    .consumer(event -> action);
```

### 2. Plan Your Layout

Calculate anchor slots based on your desired layout:

```java
// Three equal columns in a 54-slot (6-row) inventory
IAButton col1 = new IAButton().size(3, 6).anchor(0);  // Slots 0-2 per row
IAButton col2 = new IAButton().size(3, 6).anchor(3);  // Slots 3-5 per row
IAButton col3 = new IAButton().size(3, 6).anchor(6);  // Slots 6-8 per row
```

### 3. Validate Sizing

```java
// Ensure your button fits in the inventory
int inventoryRows = 6;  // 54 slots
int maxSlot = (inventoryRows * 9) - 1;  // 53

// Check if button fits
int buttonMaxSlot = anchor + ((width - 1) + (height - 1) * 9);
if (buttonMaxSlot > maxSlot) {
  // Button doesn't fit!
}
```

### 4. Use for Large Click Areas

```java
// Good use case - large visual areas
IAButton largeButton = new IAButton().size(5, 3).anchor(2);

// Bad use case - single slot (just use InventoryButton)
IAButton singleSlot = new IAButton().size(1, 1).anchor(0);
```

### 5. Consistent Item Stacks

The same ItemStack is used for all slots in the button:

```java
IAButton button = new IAButton()
    .size(3, 2)
    .anchor(0)
    .creator(player -> {
      // This item appears in ALL occupied slots
      return new ItemStack(Material.DIAMOND_SWORD);
    })
    .consumer(event -> event.setCancelled(true));
```

---

## Method Chaining

All methods return `this` for convenient chaining:

```java
IAButton button = new IAButton()
    .size(3, 2)
    .anchor(5)
    .creator(this::createIcon)
    .consumer(this::handleClick);
```

---

## Validation

IAButton automatically validates inputs:

- **Width**: Must be between 1 and 9 (inclusive)
- **Height**: Must be between 1 and 6 (inclusive)
- **Anchor**: Must be non-negative
- **Overflow**: Automatically prevents overflow beyond 9-slot width per row

```java
// These will throw IllegalArgumentException
new IAButton().size(10, 2);  // Width > 9
new IAButton().size(3, 7);   // Height > 6
new IAButton().size(0, 2);   // Width < 1
new IAButton().anchor(-1);   // Negative anchor
```

---

## Comparison with InventoryButton

### InventoryButton
**Manual slot specification:**
```java
InventoryButton button = new InventoryButton()
    .creator(player -> item)
    .consumer(event -> action);

// Must manually list all slots
addButton(button, 0, 1, 2, 9, 10, 11);
```

**Pros:**
- Simple for single slots
- Explicit control

**Cons:**
- Tedious for large areas
- Easy to miss slots
- No automatic refresh grouping

### IAButton
**Automatic multi-slot filling:**
```java
IAButton button = new IAButton()
    .size(3, 2)
    .anchor(0)
    .creator(player -> item)
    .consumer(event -> action);

// Automatically fills slots 0,1,2,9,10,11
addButton(button);
```

**Pros:**
- Easy for large areas
- Automatic slot calculation
- Smart refresh behavior
- Less error-prone

**Cons:**
- Only rectangular patterns
- Slightly more overhead

### When to Use Each

| Use Case | Recommended |
|----------|-------------|
| Single slot | `InventoryButton` |
| Few specific slots | `InventoryButton` |
| Large rectangular area | `IAButton` |
| Non-rectangular pattern | `InventoryButton` with multiple slots |
| Dynamic refreshing area | `IAButton` (smart refresh) |

---

## API Reference

### Methods

```java
// Configuration
IAButton size(int width, int height)    // Set button size
IAButton anchor(int slot)                // Set anchor slot

// From InventoryButton
IAButton creator(Function<Player, ItemStack> iconCreator)
IAButton consumer(Consumer<InventoryClickEvent> eventConsumer)

// Getters
int getWidth()
int getHeight()
int getAnchorSlot()
List<Integer> getOccupiedSlots()  // All slots this button occupies
```

### Constraints

| Property | Min | Max | Default |
|----------|-----|-----|---------|
| Width    | 1   | 9   | 1       |
| Height   | 1   | 6   | 1       |
| Anchor   | 0   | 53  | 0       |

---

## See Also

- **[Main MenuAPI Guide](MENUAPI_GUIDE.md)** - Complete MenuAPI documentation
- **[Auto-Refresh Guide](AUTO_REFRESH_GUIDE.md)** - Dynamic GUI updates
- **[Global GUI Management](GLOBAL_GUI_MANAGEMENT.md)** - Managing multiple GUIs