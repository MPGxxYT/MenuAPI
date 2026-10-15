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
    <version>v2.1.0</version>
</dependency>
```

**Gradle**

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.MPGxxYT:MenuAPI:v2.1.0'
}
```

To build from the latest commit on `master`, use the version `master-SNAPSHOT`. Snapshot builds can change without notice.

### Shading

MenuAPI is a library, not a plugin, so the server won't load it on its own. `plugin.yml`'s `libraries` section can't load it either, since that only downloads from Maven Central. Bundle (shade) MenuAPI into your plugin's jar, or the server throws `NoClassDefFoundError` the first time a menu opens.

Relocate it while shading. Relocation moves your copy of MenuAPI into your plugin's own package, so it can't clash with another plugin on the same server that bundles a different MenuAPI version.

**Maven**

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.1</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <relocations>
                            <relocation>
                                <pattern>me.mortaldev.menuapi</pattern>
                                <shadedPattern>com.example.myplugin.libs.menuapi</shadedPattern>
                            </relocation>
                        </relocations>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

Keep the Paper API dependency at `provided` scope so it isn't bundled too.

**Gradle**

```groovy
plugins {
    id 'com.gradleup.shadow' version '8.3.5'
}

shadowJar {
    relocate 'me.mortaldev.menuapi', 'com.example.myplugin.libs.menuapi'
}
```

Use the jar from `shadowJar` on your server, and keep Paper as `compileOnly`.

Replace `com.example.myplugin` with your plugin's own package. Your code still imports `me.mortaldev.menuapi`; the build rewrites those references in the jar.

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

`decorate` runs when the menu opens, and again on every `rebuild()`. MenuAPI draws the buttons after it returns, so there is no need to call `super.decorate(player)`. Add buttons in `decorate`, not the constructor: a rebuild removes every button before calling `decorate` again.

> [!NOTE]
> `openGUI` opens a menu instance once; opening it a second time throws `IllegalStateException`. To show it again with its current state, call `rebuild()` on it.

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

### Rebuilding

`rebuild()` builds a menu again from its own fields: `createInventory` and `decorate` run again, and the buttons are replaced. Use it when a click changes what the menu shows, such as a page, a filter or a search. The menu keeps its fields, so managers passed to its constructor don't need to be passed again.

```java
public class StaffMenu extends InventoryGUI {
    private final StaffManager staffManager;
    private int page = 1;

    public StaffMenu(StaffManager staffManager) {
        this.staffManager = staffManager;
    }

    @Override
    public void decorate(Player player) {
        List<Staff> staff = staffManager.page(page);   // loaded on every rebuild
        // ... add a button per staff member ...
        addButton(53, new InventoryButton()
            .creator(p -> new ItemStack(Material.ARROW))
            .consumer(event -> {
                page++;
                rebuild();
            }));
    }
}
```

- If the menu is open and its size is unchanged, it is redrawn in the inventory on screen, so the cursor does not move. A title change alone is not shown in this case.
- If the size changed, the new inventory opens. The session continues, and `onOpen` and `onClose` are not called.
- If the menu was closed, for example by an anvil prompt, it opens again as a new session and `onOpen` is called. This also replaces any other menu the player has open.
- If the player is offline, nothing happens.

Data loaded in the constructor is not loaded again; load anything that must stay current in `createInventory` or `decorate`. Calling `rebuild()` from inside either of them throws `IllegalStateException`.

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

Each tick calls `refreshGUI()`, which redraws the buttons by default. To rebuild the menu instead, for example when the number of items changes, override it and call `rebuild()`. The refresh task keeps running:

```java
@Override
protected void refreshGUI() {
    rebuild();
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

Call these, along with `openGUI` and `rebuild()`, from the main thread. To use data loaded asynchronously, switch back with `Bukkit.getScheduler().runTask(...)` first.

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
> v2.0.0 contains breaking changes. The [changelog](CHANGELOG.md#200) lists each one and what to use instead.

Every release's changes are in [CHANGELOG.md](CHANGELOG.md).

## Documentation

- [Getting started](docs/GETTING_STARTED.md): your first menu, step by step, with best practices
- [API guide](docs/MENUAPI_GUIDE.md): every feature, with examples
- [Auto-refresh guide](docs/AUTO_REFRESH_GUIDE.md)
- [Area buttons (IAButton)](docs/IABUTTON_USAGE.md)
- [Managing open menus](docs/GLOBAL_GUI_MANAGEMENT.md)
- [Changelog](CHANGELOG.md)
- [Testing guide](TESTING.md)

## Building from source

```bash
mvn package
```

Tests use JUnit 5 and MockBukkit:

```bash
mvn test
```
