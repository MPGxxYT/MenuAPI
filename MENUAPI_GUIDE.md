# MenuAPI Usage Guide

A comprehensive guide to using MenuAPI for creating custom inventory GUIs in Bukkit/Spigot plugins.

## Table of Contents
- [Getting Started](#getting-started)
- [Basic InventoryGUI](#basic-inventorygui)
- [InventoryButton](#inventorybutton)
- [IAButton (Area Buttons)](#iabutton-area-buttons)
- [GUIManager](#guimanager)
- [ItemsAdder Integration](#itemsadder-integration)
- [Advanced Features](#advanced-features)
- [Auto-Refresh System](#auto-refresh-system)
- [Global GUI Management](#global-gui-management)
- [Best Practices](#best-practices)

## Additional Guides
- **[IAButton Usage Guide](IABUTTON_USAGE.md)** - Detailed guide for multi-slot area buttons
- **[Auto-Refresh Guide](AUTO_REFRESH_GUIDE.md)** - Complete auto-refresh system documentation
- **[Global GUI Management Guide](GLOBAL_GUI_MANAGEMENT.md)** - Managing multiple GUIs across players

---

## Getting Started

MenuAPI provides a simple, object-oriented way to create custom inventory GUIs with buttons, click handlers, and event management.

### Core Components

1. **InventoryGUI** - Abstract base class for creating custom GUIs
2. **InventoryButton** - Represents a clickable button with an icon and action
3. **GUIManager** - Singleton that manages active GUIs and dispatches events
4. **ItemsAdderInventoryGUI** - Specialized GUI for ItemsAdder custom textures

---

## Basic InventoryGUI

### Creating a Simple Menu

```java
public class MyMenu extends InventoryGUI {

  @Override
  protected Inventory createInventory() {
    // Create and return a Bukkit inventory
    return Bukkit.createInventory(null, 27, "My Custom Menu");
  }

  @Override
  public void decorate(Player player) {
    // Add buttons to the GUI
    addButton(10, createMyButton());
    addButton(13, createAnotherButton());

    // Don't forget to call super.decorate(player)
    super.decorate(player);
  }

  private InventoryButton createMyButton() {
    return new InventoryButton()
        .creator(player -> {
          // Create the ItemStack that will be displayed
          return new ItemStack(Material.DIAMOND);
        })
        .consumer(event -> {
          // Handle the click event
          event.setCancelled(true);
          Player player = (Player) event.getWhoClicked();
          player.sendMessage("You clicked the diamond!");
        });
  }

  private InventoryButton createAnotherButton() {
    return new InventoryButton()
        .creator(player -> new ItemStack(Material.EMERALD))
        .consumer(event -> {
          event.setCancelled(true);
          ((Player) event.getWhoClicked()).sendMessage("You clicked the emerald!");
        });
  }
}
```

### Opening the Menu

```java
// Simple one-liner to open the menu for a player
GUIManager.getInstance().openGUI(new MyMenu(), player);
```

---

## InventoryButton

### Button Structure

An `InventoryButton` has two components:

1. **Icon Creator** - A function that creates the ItemStack displayed in the GUI
2. **Event Consumer** - A function that handles click events

### Creating Buttons

#### Basic Button
```java
InventoryButton button = new InventoryButton()
    .creator(player -> new ItemStack(Material.DIAMOND))
    .consumer(event -> {
      event.setCancelled(true);
      Player player = (Player) event.getWhoClicked();
      player.sendMessage("Clicked!");
    });
```

#### Player-Specific Button
```java
InventoryButton playerButton = new InventoryButton()
    .creator(player -> {
      // Create different items based on the player
      ItemStack item = new ItemStack(Material.PLAYER_HEAD);
      ItemMeta meta = item.getItemMeta();
      meta.setDisplayName(player.getName());
      item.setItemMeta(meta);
      return item;
    })
    .consumer(event -> {
      Player player = (Player) event.getWhoClicked();
      player.sendMessage("Hello, " + player.getName() + "!");
    });
```

#### Close Button
```java
InventoryButton closeButton = new InventoryButton()
    .creator(player -> {
      ItemStack item = new ItemStack(Material.BARRIER);
      ItemMeta meta = item.getItemMeta();
      meta.setDisplayName("§cClose");
      item.setItemMeta(meta);
      return item;
    })
    .consumer(event -> {
      event.setCancelled(true);
      event.getWhoClicked().closeInventory();
    });
```

### Adding Buttons to GUI

#### Single Slot
```java
addButton(10, myButton);
```

#### Multiple Slots (Same Button)
```java
// Add the same button to slots 0, 1, and 2
addButton(myButton, 0, 1, 2);
```

```java
// Add a border of the same button
addButton(borderButton, 0, 1, 2, 3, 4, 5, 6, 7, 8);
```

---

## IAButton (Area Buttons)

`IAButton` extends `InventoryButton` to support filling multiple slots in a rectangular pattern. Perfect for creating large clickable areas.

### Quick Example

```java
// Create a 3x2 button (3 wide, 2 tall) starting at slot 0
IAButton largeButton = new IAButton()
    .size(3, 2)              // 3 columns, 2 rows
    .anchor(0)               // Top-left corner at slot 0
    .creator(player -> new ItemStack(Material.DIAMOND_SWORD))
    .consumer(event -> {
        event.setCancelled(true);
        player.sendMessage("Large button clicked!");
    });

// Automatically fills slots: 0, 1, 2, 9, 10, 11
addButton(largeButton);
```

### Features
- **Automatic slot calculation** - Fills all slots based on size and anchor
- **IAButton-aware refreshing** - Refreshing any slot refreshes the entire button
- **Method chaining** - Clean, fluent API

See **[IAButton Usage Guide](IABUTTON_USAGE.md)** for detailed examples and advanced usage.

---

## GUIManager

The `GUIManager` is a singleton that handles all GUI operations and event routing.

### Opening a GUI
```java
GUIManager.getInstance().openGUI(new MyMenu(), player);
```

### Registering a Custom Inventory
If you need to manually register an inventory (advanced usage):
```java
Inventory inventory = /* your inventory */;
InventoryHandler handler = /* your handler */;
GUIManager.getInstance().registerHandledInventory(inventory, handler);
```

### Unregistering an Inventory
```java
GUIManager.getInstance().unregisterInventory(inventory);
```

**Note:** GUIs are automatically unregistered when the inventory is closed.

---

## ItemsAdder Integration

MenuAPI provides seamless integration with ItemsAdder's custom GUI textures.

### Method 1: Class-Based (Recommended)

This method provides the cleanest, most organized code structure.

```java
public class MyItemsAdderMenu extends ItemsAdderInventoryGUI {

  @Override
  protected Object createItemsAdderWrapper(Player player) {
    // Create the ItemsAdder GUI with custom textures
    FontImageWrapper fontImageWrapper = new FontImageWrapper("mynamespace:my_gui");
    TexturedInventoryWrapper inventoryWrapper =
        new TexturedInventoryWrapper(null, 54, "", 32, -48, fontImageWrapper);
    // Return the wrapper - do NOT call showInventory()
    return inventoryWrapper;
  }

  @Override
  public void decorate(Player player) {
    // Add your buttons
    addButton(10, createPlayButton());
    addButton(13, createSettingsButton());

    // Multiple slots example
    addButton(createBorderButton(), 0, 1, 2, 3, 4, 5, 6, 7, 8);

    super.decorate(player);
  }

  private InventoryButton createPlayButton() {
    return new InventoryButton()
        .creator(player -> {
          CustomStack custom = CustomStack.getInstance("mynamespace:play_icon");
          ItemStack item = custom.getItemStack();
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§a§lPlay!");
          meta.setLore(Arrays.asList("§7Click to start playing"));
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> {
          event.setCancelled(true);
          Player p = (Player) event.getWhoClicked();
          p.sendMessage("§aStarting game...");
          p.closeInventory();
        });
  }

  private InventoryButton createSettingsButton() {
    return new InventoryButton()
        .creator(player -> {
          CustomStack custom = CustomStack.getInstance("mynamespace:settings_icon");
          return custom.getItemStack();
        })
        .consumer(event -> {
          event.setCancelled(true);
          // Open settings menu
        });
  }

  private InventoryButton createBorderButton() {
    return new InventoryButton()
        .creator(player -> {
          CustomStack custom = CustomStack.getInstance("mynamespace:border");
          return custom.getItemStack();
        })
        .consumer(event -> event.setCancelled(true));
  }
}
```

**Opening the menu:**
```java
GUIManager.getInstance().openGUI(new MyItemsAdderMenu(), player);
```

### Method 2: Direct Usage

For simple, one-off GUIs without a dedicated class.

```java
// Create the ItemsAdder GUI
FontImageWrapper fontImageWrapper = new FontImageWrapper("mynamespace:simple_gui");
TexturedInventoryWrapper wrapper =
    new TexturedInventoryWrapper(null, 54, "", 32, -48, fontImageWrapper);
wrapper.showInventory(player);
Inventory inventory = player.getOpenInventory().getTopInventory();

// Create the MenuAPI wrapper
ItemsAdderInventoryGUI gui = new ItemsAdderInventoryGUI(inventory);

// Add buttons
InventoryButton button = new InventoryButton()
    .creator(p -> new ItemStack(Material.DIAMOND))
    .consumer(e -> {
      e.setCancelled(true);
      ((Player) e.getWhoClicked()).sendMessage("Clicked!");
    });

gui.addButton(button, 10, 11, 12);

// Register with GUIManager
GUIManager.getInstance().registerHandledInventory(inventory, gui);

// Decorate the inventory
gui.decorate(player);
```

---

## Advanced Features

### Click Permissions

Control whether clicks are allowed in different parts of the inventory.

```java
public class MyMenu extends InventoryGUI {

  public MyMenu() {
    super();
    // Allow clicks in the player's inventory (bottom)
    allowBottomInventoryClick(true);

    // Disallow clicks in the GUI itself (top) - this is default
    allowTopInventoryClick(false);
  }

  // ... rest of implementation
}
```

### Refreshing Buttons

Update button displays without recreating the entire GUI.

```java
public class DynamicMenu extends InventoryGUI {

  // Refresh all buttons
  public void updateAll() {
    refreshButtons();
  }

  // Refresh a specific button at slot 10
  public void updateSlot10() {
    refreshButton(10);
  }
}
```

### Accessing the Registered Player

Get the player who opened the GUI (useful in button creators/consumers).

```java
@Override
public void decorate(Player player) {
  // You can access the registered player
  Player registeredPlayer = getRegisteredPlayer();

  // ... add buttons
  super.decorate(player);
}
```

### Custom Event Handlers

Override the default event handlers for custom behavior.

```java
public class CustomMenu extends InventoryGUI {

  @Override
  public void onClick(InventoryClickEvent event) {
    // Custom click handling
    Player player = (Player) event.getWhoClicked();
    player.sendMessage("You clicked slot " + event.getSlot());

    // Call super to maintain button functionality
    super.onClick(event);
  }

  @Override
  public void onOpen(InventoryOpenEvent event) {
    super.onOpen(event);
    // Custom open logic
    event.getPlayer().sendMessage("§aWelcome to the menu!");
  }

  @Override
  public void onClose(InventoryCloseEvent event) {
    super.onClose(event);
    // Custom close logic
    event.getPlayer().sendMessage("§cMenu closed!");
  }
}
```

### Accessing the Button Map

For advanced manipulation of buttons.

```java
public class AdvancedMenu extends InventoryGUI {

  protected void manipulateButtons() {
    Map<Integer, InventoryButton> buttonMap = getButtonMap();

    // Example: Remove a button
    buttonMap.remove(10);

    // Example: Check if a slot has a button
    if (buttonMap.containsKey(13)) {
      // Do something
    }
  }
}
```

---

## Auto-Refresh System

MenuAPI includes a built-in auto-refresh system that automatically updates GUIs at specified intervals.

### Basic Usage

```java
public class PlayMenu extends InventoryGUI {

  public PlayMenu() {
    super();
    // Enable auto-refresh: refresh every 2 seconds (40 ticks)
    enableAutoRefresh(MyPlugin.getInstance(), 40L);
  }

  @Override
  protected Inventory createInventory() {
    return Bukkit.createInventory(null, 54, "Play Menu");
  }

  @Override
  public void decorate(Player player) {
    // Add buttons that show dynamic data
    addButton(10, playerCountButton());
    super.decorate(player);
  }

  // This button will auto-refresh every 2 seconds
  private InventoryButton playerCountButton() {
    return new InventoryButton()
        .creator(player -> {
          int count = Bukkit.getOnlinePlayers().size();
          ItemStack item = new ItemStack(Material.PLAYER_HEAD);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§e" + count + " Players Online");
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> event.setCancelled(true));
  }
}
```

### Features
- **Automatic lifecycle management** - Starts on open, stops on close
- **Player validation** - Automatically stops if player goes offline
- **Customizable refresh behavior** - Override `refreshGUI()` for custom logic
- **IAButton support** - Area buttons refresh correctly

### Custom Refresh Logic

```java
@Override
protected void refreshGUI() {
  // Option 1: Simple refresh (default) - just update button displays
  refreshButtons();

  // Option 2: Full refresh - recreate the entire menu
  stopAutoRefresh();
  GUIManager.getInstance().openGUI(new PlayMenu(), getRegisteredPlayer());
}
```

See **[Auto-Refresh Guide](AUTO_REFRESH_GUIDE.md)** for complete documentation and advanced examples.

---

## Global GUI Management

Track and manage all open GUIs across all players with static methods.

### Refreshing All GUIs of a Type

```java
// When data changes that affects a specific GUI type
public class LobbyManager {

  public void createLobby(String name) {
    // ... create lobby logic ...

    // Refresh all open LobbiesMenu instances
    InventoryGUI.refreshAllGUIsOfType(LobbiesMenu.class);
  }
}
```

### Checking Player GUI Status

```java
// Check if a player has a GUI open
if (InventoryGUI.hasOpenGUI(player)) {
  player.sendMessage("You already have a menu open!");
  return;
}

// Get the specific GUI a player has open
InventoryGUI gui = InventoryGUI.getOpenGUI(player);
if (gui instanceof PlayMenu) {
  player.sendMessage("You're viewing the play menu!");
}
```

### Available Static Methods

- `refreshAllGUIsOfType(Class)` - Full refresh (calls `refreshGUI()`)
- `refreshButtonsForType(Class)` - Button display refresh only (more efficient)
- `getOpenGUI(Player)` - Get player's current GUI
- `hasOpenGUI(Player)` - Check if player has GUI open

See **[Global GUI Management Guide](GLOBAL_GUI_MANAGEMENT.md)** for detailed examples.

---

## Best Practices

### 1. Organize Buttons in Separate Methods

```java
public class WellOrganizedMenu extends InventoryGUI {

  @Override
  public void decorate(Player player) {
    addButton(10, playButton());
    addButton(13, settingsButton());
    addButton(16, quitButton());
    super.decorate(player);
  }

  private InventoryButton playButton() {
    return new InventoryButton()
        .creator(this::createPlayIcon)
        .consumer(this::handlePlayClick);
  }

  private ItemStack createPlayIcon(Player player) {
    // Icon creation logic
    return new ItemStack(Material.DIAMOND_SWORD);
  }

  private void handlePlayClick(InventoryClickEvent event) {
    event.setCancelled(true);
    // Click handling logic
  }

  // ... more button methods
}
```

### 2. Use Constants for Slot Numbers

```java
public class MyMenu extends InventoryGUI {

  private static final int SLOT_PLAY = 10;
  private static final int SLOT_SETTINGS = 13;
  private static final int SLOT_QUIT = 16;

  @Override
  public void decorate(Player player) {
    addButton(SLOT_PLAY, playButton());
    addButton(SLOT_SETTINGS, settingsButton());
    addButton(SLOT_QUIT, quitButton());
    super.decorate(player);
  }
}
```

### 3. Always Call super.decorate(player)

```java
@Override
public void decorate(Player player) {
  addButton(10, myButton());
  // This is required to actually place the buttons in the inventory
  super.decorate(player);
}
```

### 4. Cancel Click Events

```java
.consumer(event -> {
  // Always cancel the event to prevent item pickup
  event.setCancelled(true);

  // Your logic here
})
```

### 5. Close Inventory When Navigating

```java
.consumer(event -> {
  event.setCancelled(true);
  Player player = (Player) event.getWhoClicked();

  // Close the current inventory before opening another
  player.closeInventory();

  // Then open the new one
  GUIManager.getInstance().openGUI(new OtherMenu(), player);
})
```

### 6. Use Method References for Cleaner Code

```java
// Instead of:
.creator(player -> createIcon(player))
.consumer(event -> handleClick(event))

// Use:
.creator(this::createIcon)
.consumer(this::handleClick)
```

---

## Complete Examples

### Example 1: Game Mode Selection Menu

```java
public class GameModeMenu extends InventoryGUI {

  @Override
  protected Inventory createInventory() {
    return Bukkit.createInventory(null, 27, "§6§lSelect Game Mode");
  }

  @Override
  public void decorate(Player player) {
    addButton(11, gameModeButton(GameMode.SURVIVAL));
    addButton(13, gameModeButton(GameMode.CREATIVE));
    addButton(15, gameModeButton(GameMode.ADVENTURE));
    addButton(22, closeButton());
    super.decorate(player);
  }

  private InventoryButton gameModeButton(GameMode mode) {
    return new InventoryButton()
        .creator(player -> {
          Material material = switch (mode) {
            case SURVIVAL -> Material.IRON_SWORD;
            case CREATIVE -> Material.DIAMOND_BLOCK;
            case ADVENTURE -> Material.MAP;
            default -> Material.BARRIER;
          };

          ItemStack item = new ItemStack(material);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§e" + mode.name());
          meta.setLore(Arrays.asList("§7Click to switch to", "§7" + mode.name() + " mode"));
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> {
          event.setCancelled(true);
          Player player = (Player) event.getWhoClicked();
          player.setGameMode(mode);
          player.sendMessage("§aGame mode changed to " + mode.name());
          player.closeInventory();
        });
  }

  private InventoryButton closeButton() {
    return new InventoryButton()
        .creator(player -> {
          ItemStack item = new ItemStack(Material.BARRIER);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§cClose");
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> {
          event.setCancelled(true);
          event.getWhoClicked().closeInventory();
        });
  }
}
```

### Example 2: Paginated Menu

```java
public class PaginatedMenu extends InventoryGUI {

  private final List<ItemStack> items;
  private int page = 0;
  private static final int ITEMS_PER_PAGE = 21;

  public PaginatedMenu(List<ItemStack> items) {
    this.items = items;
  }

  @Override
  protected Inventory createInventory() {
    return Bukkit.createInventory(null, 54, "§6§lPage " + (page + 1));
  }

  @Override
  public void decorate(Player player) {
    // Display items for current page
    int startIndex = page * ITEMS_PER_PAGE;
    int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

    int slot = 10;
    for (int i = startIndex; i < endIndex; i++) {
      ItemStack item = items.get(i);
      addButton(slot++, itemButton(item));
      if (slot % 9 == 8) slot += 2; // Skip last column
    }

    // Navigation buttons
    if (page > 0) {
      addButton(48, previousPageButton());
    }
    if ((page + 1) * ITEMS_PER_PAGE < items.size()) {
      addButton(50, nextPageButton());
    }

    addButton(49, closeButton());
    super.decorate(player);
  }

  private InventoryButton itemButton(ItemStack item) {
    return new InventoryButton()
        .creator(player -> item)
        .consumer(event -> {
          event.setCancelled(true);
          // Handle item click
        });
  }

  private InventoryButton previousPageButton() {
    return new InventoryButton()
        .creator(player -> {
          ItemStack item = new ItemStack(Material.ARROW);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§e← Previous Page");
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> {
          event.setCancelled(true);
          page--;
          // Refresh the GUI with new page
          refreshButtons();
        });
  }

  private InventoryButton nextPageButton() {
    return new InventoryButton()
        .creator(player -> {
          ItemStack item = new ItemStack(Material.ARROW);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§eNext Page →");
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> {
          event.setCancelled(true);
          page++;
          refreshButtons();
        });
  }

  private InventoryButton closeButton() {
    return new InventoryButton()
        .creator(player -> {
          ItemStack item = new ItemStack(Material.BARRIER);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§cClose");
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> {
          event.setCancelled(true);
          event.getWhoClicked().closeInventory();
        });
  }
}
```

---

## Troubleshooting

### Buttons Not Appearing
- Make sure you're calling `super.decorate(player)` at the end of your `decorate()` method
- Verify that your icon creator is returning a non-null ItemStack

### Clicks Not Working
- Ensure the GUIListener is registered in your plugin's onEnable
- Check that you're using `GUIManager.getInstance().openGUI()` to open the menu
- Verify that event.setCancelled(true) is called in your button consumers

### ItemsAdder GUI Not Working
- Ensure ItemsAdder is loaded before creating the GUI
- Verify the namespace ID is correct
- Make sure you're returning the inventory from `player.getOpenInventory().getTopInventory()`

### Player Not Registered Error
- Don't call `getRegisteredPlayer()` in the constructor
- Use it only in `createInventory()`, `decorate()`, or after the GUI is opened

---

## Dependencies

MenuAPI has no external dependencies beyond Bukkit/Spigot API. ItemsAdder integration is optional and only required if you're using `ItemsAdderInventoryGUI`.

---

## License

MenuAPI is provided as-is for use in Bukkit/Spigot plugins.