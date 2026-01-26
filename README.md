# MenuAPI

A lightweight, object-oriented library for creating interactive inventory GUIs in Bukkit/Paper plugins.

## Features

- **Fluent Button API** - Create buttons with method chaining for clean, readable code
- **Single & Multi-Slot Buttons** - Support for single-slot buttons and area buttons spanning multiple slots
- **Auto-Refresh System** - Automatically update dynamic content at configurable intervals
- **Global GUI Management** - Track and refresh all open GUIs across players
- **ItemsAdder Integration** - Optional support for custom textures and HUDs (no hard dependency)
- **Lifecycle Management** - Automatic cleanup of tasks and player references

## Documentation

- [Complete API Guide](docs/MENUAPI_GUIDE.md) - Comprehensive guide covering all features
- [Auto-Refresh Guide](docs/AUTO_REFRESH_GUIDE.md) - Detailed auto-refresh system documentation
- [IAButton Usage](docs/IABUTTON_USAGE.md) - Multi-slot area button guide
- [Global GUI Management](docs/GLOBAL_GUI_MANAGEMENT.md) - Managing GUIs across players

## Installation

Add the dependency to your `pom.xml`:

<details>
<summary>Maven Dependency</summary>

```xml
<dependency>
    <groupId>me.mortaldev</groupId>
    <artifactId>MenuAPI</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

</details>

## Setup

Register the event listener in your plugin's `onEnable()`:

<details>
<summary>Plugin Setup</summary>

```java
@Override
public void onEnable() {
    GUIListener listener = new GUIListener(GUIManager.getInstance());
    getServer().getPluginManager().registerEvents(listener, this);
}
```

</details>

## Quick Start

### Creating a Basic Menu

<details>
<summary>Example Menu Class</summary>

```java
public class MainMenu extends InventoryGUI {

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "Main Menu");
    }

    @Override
    public void decorate(Player player) {
        addButton(13, new InventoryButton()
            .creator(p -> {
                ItemStack item = new ItemStack(Material.DIAMOND);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("Click Me!");
                item.setItemMeta(meta);
                return item;
            })
            .consumer(event -> {
                event.setCancelled(true);
                Player clicker = (Player) event.getWhoClicked();
                clicker.sendMessage("Button clicked!");
            }));

        super.decorate(player);
    }
}
```

</details>

### Opening the Menu

```java
GUIManager.getInstance().openGUI(new MainMenu(), player);
```

## Core Components

### InventoryButton

A single-slot button with an icon and click handler.

<details>
<summary>Usage Example</summary>

```java
InventoryButton button = new InventoryButton()
    .creator(player -> new ItemStack(Material.EMERALD))  // Icon generator
    .consumer(event -> {                                  // Click handler
        event.setCancelled(true);
        // Handle click
    });

// Add to a single slot
addButton(10, button);

// Add to multiple slots
addButton(button, 10, 11, 12);
```

</details>

### IAButton (Inventory Area Button)

A button that spans multiple slots in a rectangular area.

<details>
<summary>Usage Example</summary>

```java
IAButton areaButton = new IAButton()
    .size(3, 2)      // Width x Height (max 9x6)
    .anchor(0)       // Top-left slot position
    .creator(player -> new ItemStack(Material.DIAMOND_SWORD))
    .consumer(event -> {
        event.setCancelled(true);
        // Handles clicks on any slot in the area
    });

addButton(areaButton);
```

</details>

### GUIManager

Singleton that manages all active GUIs and handles event routing.

<details>
<summary>Usage Example</summary>

```java
GUIManager manager = GUIManager.getInstance();

// Open a GUI
manager.openGUI(new MyMenu(), player);

// Check if player has a GUI open
if (InventoryGUI.hasOpenGUI(player)) {
    InventoryGUI gui = InventoryGUI.getOpenGUI(player);
}
```

</details>

## Auto-Refresh

Enable automatic button updates for dynamic content.

<details>
<summary>Auto-Refresh Example</summary>

```java
public class LiveStatsMenu extends InventoryGUI {

    public LiveStatsMenu() {
        super();
        enableAutoRefresh(MyPlugin.getInstance(), 20L);  // Refresh every 20 ticks (1 second)
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "Live Stats");
    }

    @Override
    public void decorate(Player player) {
        addButton(13, new InventoryButton()
            .creator(p -> {
                int online = Bukkit.getOnlinePlayers().size();
                ItemStack item = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("Players: " + online);
                item.setItemMeta(meta);
                return item;
            })
            .consumer(e -> e.setCancelled(true)));

        super.decorate(player);
    }
}
```

Auto-refresh starts when the GUI opens and stops automatically when it closes.

</details>

## Global GUI Management

Refresh all instances of a specific menu type across all players:

<details>
<summary>Usage Example</summary>

```java
// Refresh entire GUI (re-runs decorate)
InventoryGUI.refreshAllGUIsOfType(LobbyMenu.class);

// Refresh only button displays (more efficient)
InventoryGUI.refreshButtonsForType(LobbyMenu.class);
```

</details>

## Click Control

Control where players can click:

<details>
<summary>Usage Example</summary>

```java
public class MyMenu extends InventoryGUI {

    public MyMenu() {
        super();
        allowBottomInventoryClick(true);   // Allow clicks in player inventory
        allowTopInventoryClick(false);     // Disallow clicks in GUI (default)
    }

    // ...
}
```

</details>

## Manual Button Refresh

Refresh specific buttons programmatically:

<details>
<summary>Usage Example</summary>

```java
// Refresh a single slot
refreshButton(13);

// Refresh all buttons
refreshButtons();
```

</details>

## ItemsAdder Integration

For custom textures using ItemsAdder:

<details>
<summary>Usage Example</summary>

```java
public class CustomTextureMenu extends ItemsAdderInventoryGUI {

    @Override
    protected Object createItemsAdderWrapper(Player player) {
        FontImageWrapper font = new FontImageWrapper("namespace:my_gui");
        return new TexturedInventoryWrapper(null, 54, "", 32, -48, font);
    }

    @Override
    public void decorate(Player player) {
        addButton(10, myButton());
        super.decorate(player);
    }
}
```

</details>

## Event Handling

Override methods to handle inventory events:

<details>
<summary>Usage Example</summary>

```java
public class MyMenu extends InventoryGUI {

    @Override
    public void onClick(InventoryClickEvent event) {
        // Called on every click (before button handlers)
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        // Called when GUI opens
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        // Called when GUI closes
    }

    // ...
}
```

</details>

## Requirements

- Java 21+
- Paper API 1.19.4+

## License

This project is available for use in your Bukkit/Paper plugins.
