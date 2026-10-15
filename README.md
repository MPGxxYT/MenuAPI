<div align="center">

# MenuAPI

[![JitPack](https://jitpack.io/v/MPGxxYT/MenuAPI.svg)](https://jitpack.io/#MPGxxYT/MenuAPI)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Paper 1.19.4+](https://img.shields.io/badge/Paper-1.19.4%2B-blue)

A library for building clickable inventory menus in Bukkit/Paper plugins.

[Installation](#installation) | [Quick start](#quick-start) | [Usage](#usage) | [Documentation](#documentation)

</div>

You describe a menu as a class: the inventory it uses and the buttons it shows. MenuAPI opens it for a player, draws the buttons, cancels clicks you didn't allow, runs button handlers, and cleans up when the menu closes.

## Features

- Buttons built with method chaining: an icon creator and a click handler
- Area buttons that cover a rectangle of slots and act as one button
- Auto-refresh on a tick interval, started and stopped with the menu
- Lookup and refresh of open menus by type, across all players
- Optional ItemsAdder support for textured menus and HUDs, with no compile-time dependency

## Requirements

- Java 21
- Paper API 1.19.4 or later

## Installation

MenuAPI is published through [JitPack](https://jitpack.io/#MPGxxYT/MenuAPI).

**Maven**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.MPGxxYT</groupId>
    <artifactId>MenuAPI</artifactId>
    <version>v2.0.0</version>
</dependency>
```

**Gradle**

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.MPGxxYT:MenuAPI:v2.0.0'
}
```

To build from the latest commit on `master`, use the version `master-SNAPSHOT`. Snapshot builds can change without notice.

## Quick start

Register the listener once, in your plugin's `onEnable()`:

```java
@Override
public void onEnable() {
    getServer().getPluginManager().registerEvents(
        new GUIListener(GUIManager.getInstance()), this);
}
```

> [!IMPORTANT]
> Without the listener, clicks and closes never reach MenuAPI: buttons do nothing and clicks are not cancelled.

Create a menu by extending `InventoryGUI`:

```java
public class MainMenu extends InventoryGUI {

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "Main Menu");
    }

    @Override
    public void decorate(Player player) {
        addButton(13, new InventoryButton()
            .creator(p -> new ItemStack(Material.DIAMOND))
            .consumer(event -> event.getWhoClicked().sendMessage("Button clicked!")));
    }
}
```

Open it:

```java
GUIManager.getInstance().openGUI(new MainMenu(), player);
```

`decorate` runs once when the menu opens. MenuAPI draws the buttons after it returns, so there is no need to call `super.decorate(player)`.

> [!NOTE]
> A menu instance can be opened once. To show the same menu again, create a new instance. Opening an instance a second time throws `IllegalStateException`.

## Usage

### Buttons

An `InventoryButton` pairs an icon creator with a click handler. The creator receives the player the menu is open for.

```java
InventoryButton button = new InventoryButton()
    .creator(player -> new ItemStack(Material.EMERALD))
    .consumer(event -> { /* handle the click */ });

addButton(10, button);            // one slot
addButton(button, 10, 11, 12);    // several slots, same button
```

An `IAButton` covers a rectangle of slots, up to 9 wide and 6 tall, starting at its top-left `anchor` slot:

```java
addButton(new IAButton()
    .size(3, 2)
    .anchor(0)
    .creator(player -> new ItemStack(Material.DIAMOND_SWORD))
    .consumer(event -> { /* any slot in the area */ }));
```

An area that runs past the right edge of the inventory throws `IllegalArgumentException` when it is added. A button placed past the end of the inventory throws when the menu is drawn.

To change buttons while a menu is open:

```java
refreshButton(13);      // redraw one button, in every slot it covers
refreshButtons();       // redraw all buttons
removeButton(10);       // remove a button and clear its icon
getButton(13);          // the button at a slot, or null
```

### Clicks and hooks

By default, clicks in both the menu and the player's own inventory are cancelled. Button handlers still run. To allow clicks:

```java
public MyMenu() {
    allowTopInventoryClick(true);       // clicks in the menu
    allowBottomInventoryClick(true);    // clicks in the player's inventory
}
```

`onOpen`, `onClick` and `onClose` are optional hooks; none of them need to call `super`.

| Hook | Runs |
| --- | --- |
| `onOpen(InventoryOpenEvent)` | After the menu is on screen and its buttons are drawn. Cancelling the event has no effect. |
| `onClick(InventoryClickEvent)` | On every click, after the click rules above and before the button's handler. |
| `onClose(InventoryCloseEvent)` | When the menu closes, before MenuAPI stops tracking it. |

### Auto-refresh

Call `enableAutoRefresh` in the constructor to redraw the menu on a tick interval:

```java
public LiveStatsMenu() {
    enableAutoRefresh(MyPlugin.getInstance(), 20L);   // every second
}
```

The task starts when the menu opens and stops when it closes, when the player opens another menu, or when the player goes offline. `disableAutoRefresh()` stops it while the menu stays open.

Each tick calls `refreshGUI()`, which redraws the buttons by default. To rebuild the menu instead, for example when the number of items changes, override it and open a new instance:

```java
@Override
protected void refreshGUI() {
    GUIManager.getInstance().openGUI(new LobbiesMenu(), getRegisteredPlayer());
}
```

### Open menus across players

```java
GUIManager manager = GUIManager.getInstance();

manager.getOpenGUI(player);                     // the player's open menu, or null
manager.hasOpenGUI(player);
manager.refreshAllGUIsOfType(LobbyMenu.class);  // calls refreshGUI() on each
manager.refreshButtonsForType(LobbyMenu.class); // redraws buttons only
```

### ItemsAdder

Extend `ItemsAdderInventoryGUI` and return a `TexturedInventoryWrapper` from `createItemsAdderWrapper`. Don't call `showInventory` yourself; MenuAPI does that when the menu opens.

```java
public class CustomTextureMenu extends ItemsAdderInventoryGUI {

    @Override
    protected Object createItemsAdderWrapper(Player player) {
        FontImageWrapper font = new FontImageWrapper("namespace:my_gui");
        return new TexturedInventoryWrapper(null, 54, "", 32, -48, font);
    }

    @Override
    protected List<String> getHudIds(Player player) {
        return List.of("namespace:my_hud");   // shown while the menu is open
    }

    @Override
    public void decorate(Player player) {
        addButton(10, myButton());
    }
}
```

If ItemsAdder has already opened an inventory, wrap it with `new ItemsAdderInventoryGUI(inventory)` and pass that to `openGUI`. The inventory is not reopened.

ItemsAdder is called through reflection, so MenuAPI loads on servers without it.

## Upgrading from v1

> [!WARNING]
> v2.0.0 contains breaking changes.

- `decorate`, `onOpen`, `onClick` and `onClose` are optional hooks. Calls to `super` are no longer needed, and existing ones do nothing.
- `InventoryGUI.getOpenGUI`, `hasOpenGUI`, `refreshAllGUIsOfType` and `refreshButtonsForType` are now instance methods on `GUIManager.getInstance()`.
- `startAutoRefresh` and `stopAutoRefresh` are removed. Use `disableAutoRefresh()` to stop refreshing while a menu stays open.
- `InventoryHandler`, `registerHandledInventory` and `unregisterInventory` are removed. Open every menu with `GUIManager.getInstance().openGUI(gui, player)`.
- `GUIManager.handleOpen` is removed, and `onOpen` can no longer cancel the open.
- `getButtonMap()` is replaced by `getButton(slot)` and `removeButton(slot)`.
- A menu instance can be opened once.
- An `IAButton` that runs past the right edge throws instead of being clipped.
- An ItemsAdder wrapper whose `showInventory` throws without opening anything makes `openGUI` throw `IllegalStateException`.

## Documentation

- [API guide](docs/MENUAPI_GUIDE.md): every feature, with examples
- [Auto-refresh guide](docs/AUTO_REFRESH_GUIDE.md)
- [Area buttons (IAButton)](docs/IABUTTON_USAGE.md)
- [Managing open menus](docs/GLOBAL_GUI_MANAGEMENT.md)
- [Testing guide](TESTING.md)

## Building from source

```bash
mvn package
```

Tests use JUnit 5 and MockBukkit:

```bash
mvn test
```
