# Getting Started: Your First Menu

This guide builds a warp menu step by step: a single button first, then a list from your own data, pages, a detail screen with a Back button, and auto-refresh. Each step adds one idea, and the end of the guide collects the practices that keep menus simple as they grow.

It assumes MenuAPI is already a dependency of your plugin. See [Installation](../README.md#installation) if it isn't.

## Contents

- [1. Register the listener](#1-register-the-listener)
- [2. A menu with one button](#2-a-menu-with-one-button)
- [3. What happens when a menu opens](#3-what-happens-when-a-menu-opens)
- [4. Show your own data](#4-show-your-own-data)
- [5. Pages, filters and other state](#5-pages-filters-and-other-state)
- [6. Going to another menu and back](#6-going-to-another-menu-and-back)
- [7. Keeping a menu up to date](#7-keeping-a-menu-up-to-date)
- [Best practices](#best-practices)
- [Common mistakes](#common-mistakes)
- [Next steps](#next-steps)

## 1. Register the listener

MenuAPI receives clicks and closes through `GUIListener`. Register it once, in `onEnable()`:

```java
public final class WarpsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(
            new GUIListener(GUIManager.getInstance()), this);
    }
}
```

Without it, menus still open, but buttons do nothing and players can take items out of them.

## 2. A menu with one button

A menu is a class that extends `InventoryGUI`. It answers two questions: which inventory to show (`createInventory`) and which buttons to put in it (`decorate`).

```java
public class WarpMenu extends InventoryGUI {

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, "Warps");
    }

    @Override
    public void decorate(Player player) {
        addButton(13, new InventoryButton()
            .creator(p -> new ItemStack(Material.ENDER_PEARL))
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                clicker.teleport(clicker.getWorld().getSpawnLocation());
                clicker.closeInventory();
            }));
    }
}
```

A button has two parts:

- `creator` returns the icon. It receives the player the menu is open for, so the icon can differ per player.
- `consumer` runs when the button is clicked, and receives the Bukkit `InventoryClickEvent`.

Open the menu from a command, or anywhere you have a `Player`:

```java
GUIManager.getInstance().openGUI(new WarpMenu(), player);
```

You don't need to cancel the click: MenuAPI cancels clicks in the menu and in the player's own inventory by default, and still runs the button's handler. You also don't need to call `super` in any method you override.

## 3. What happens when a menu opens

Knowing the order helps you decide where code goes.

When you call `openGUI(menu, player)`:

1. `createInventory()` runs and the inventory opens for the player.
2. `decorate(player)` runs and adds buttons.
3. MenuAPI draws every button by calling its `creator`.
4. `onOpen(event)` runs, if you overrode it.
5. Auto-refresh starts, if you enabled it.

When the player clicks:

1. MenuAPI cancels the click, unless you allowed clicks in that inventory.
2. `onClick(event)` runs, if you overrode it.
3. The clicked button's `consumer` runs.

When the menu closes, `onClose(event)` runs and MenuAPI stops tracking the menu. Opening another menu for the same player closes the first one in the same way.

`getRegisteredPlayer()` returns the player the menu is open for. It works from `createInventory()` onwards, but not in the constructor, because the player isn't known until `openGUI` runs.

## 4. Show your own data

Most menus show something from your plugin: warps, kits, players, shop items. Pass the object that owns that data to the constructor and keep it in a field.

```java
public class WarpMenu extends InventoryGUI {

    private final WarpManager warpManager;

    public WarpMenu(WarpManager warpManager) {
        this.warpManager = warpManager;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "Warps");
    }

    @Override
    public void decorate(Player player) {
        List<Warp> warps = warpManager.getWarps();
        for (int i = 0; i < warps.size() && i < 45; i++) {
            addButton(i, warpButton(warps.get(i)));
        }
    }

    private InventoryButton warpButton(Warp warp) {
        return new InventoryButton()
            .creator(p -> warp.getIcon())
            .consumer(event -> {
                Player clicker = (Player) event.getWhoClicked();
                clicker.teleport(warp.getLocation());
                clicker.closeInventory();
            });
    }
}
```

```java
GUIManager.getInstance().openGUI(new WarpMenu(warpManager), player);
```

Two things to notice:

- The warps are read in `decorate`, not in the constructor. `decorate` runs again whenever the menu is rebuilt (next step), so the menu always shows the current list.
- Each button gets its own method. `decorate` stays a readable list of what goes where.

## 5. Pages, filters and other state

When a click changes what the menu shows, such as the page, a filter or a search, store that in a field, change the field, and call `rebuild()`:

```java
public class WarpMenu extends InventoryGUI {

    private static final int PER_PAGE = 45;
    private static final int PREVIOUS_SLOT = 45;
    private static final int NEXT_SLOT = 53;

    private final WarpManager warpManager;
    private int page = 0;

    public WarpMenu(WarpManager warpManager) {
        this.warpManager = warpManager;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 54, "Warps");
    }

    @Override
    public void decorate(Player player) {
        List<Warp> warps = warpManager.getWarps();
        int start = page * PER_PAGE;
        int end = Math.min(start + PER_PAGE, warps.size());
        for (int i = start; i < end; i++) {
            addButton(i - start, warpButton(warps.get(i)));
        }

        if (page > 0) {
            addButton(PREVIOUS_SLOT, pageButton("Previous page", -1));
        }
        if (end < warps.size()) {
            addButton(NEXT_SLOT, pageButton("Next page", 1));
        }
    }

    private InventoryButton pageButton(String name, int change) {
        return new InventoryButton()
            .creator(p -> named(Material.ARROW, name))
            .consumer(event -> {
                page += change;
                rebuild();
            });
    }

    private static ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.displayName(Component.text(name)));
        return item;
    }

    // warpButton(...) as before
}
```

`rebuild()` runs `createInventory()` and `decorate()` again on the same menu, so `page` and `warpManager` keep their values and nothing has to be passed again. Every old button is removed first, which is why the Previous button disappears on the first page.

How the player sees a rebuild:

- If the inventory size is unchanged, the menu is redrawn in place and the cursor stays where it was.
- If the size changed, the new inventory opens. The menu is still the same session, so `onOpen` and `onClose` don't run.
- A new title only shows when the size changes too. That's why this menu keeps the title as "Warps": a page number in the title would stay at page 1. Put the page number on an item, such as the arrow's lore, if it needs to be visible.

Don't open a new copy of the menu to change pages:

```java
// Avoid: every dependency has to be passed again, and the page is lost unless you pass it too
GUIManager.getInstance().openGUI(new WarpMenu(warpManager, page + 1), player);
```

## 6. Going to another menu and back

To go to another menu, open a new instance of it. To come back, keep a reference to the menu you came from and call `rebuild()` on it. A closed menu reopens with its fields as they were, so the player lands on the same page.

```java
public class WarpDetailMenu extends InventoryGUI {

    private final Warp warp;
    private final WarpManager warpManager;   // used by the delete button
    private final InventoryGUI previous;

    public WarpDetailMenu(Warp warp, WarpManager warpManager, InventoryGUI previous) {
        this.warp = warp;
        this.warpManager = warpManager;
        this.previous = previous;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, 27, warp.getName());
    }

    @Override
    public void decorate(Player player) {
        addButton(11, teleportButton());
        addButton(15, deleteButton());
        addButton(18, new InventoryButton()
            .creator(p -> named(Material.ARROW, "Back"))
            .consumer(event -> previous.rebuild()));
    }

    private InventoryButton deleteButton() {
        return new InventoryButton()
            .creator(p -> named(Material.LAVA_BUCKET, "Delete warp"))
            .consumer(event -> {
                warpManager.delete(warp);
                previous.rebuild();
            });
    }

    // teleportButton() and named(...) as in WarpMenu
}
```

In `WarpMenu`, the warp button opens the detail screen and passes itself:

```java
.consumer(event -> GUIManager.getInstance().openGUI(
    new WarpDetailMenu(warp, warpManager, this), (Player) event.getWhoClicked()))
```

The detail menu receives what it uses: the warp, the manager its delete button calls, and where Back goes. It never has to construct `WarpMenu`, so a service that only `WarpMenu` uses never has to pass through it. After a delete, `previous.rebuild()` reopens `WarpMenu` on the same page, `decorate` reads the warp list again, and the deleted warp is gone.

The same works for prompts that close the menu, such as an AnvilGUI search box: set the field in the prompt's callback and call `rebuild()`, and the menu opens again.

## 7. Keeping a menu up to date

Pick the smallest update that shows the change:

| What changed | Call | What runs |
| --- | --- | --- |
| One icon | `refreshButton(slot)` | That button's `creator` |
| Several icons, same buttons in the same slots | `refreshButtons()` | Every `creator` |
| Which buttons exist, how many, or the inventory size | `rebuild()` | `createInventory()`, `decorate()`, then every `creator` |

`refreshButtons()` only runs the `creator` functions again. A value read in `decorate` and captured by a creator won't change. Read the value inside the creator if `refreshButtons()` should pick it up:

```java
// Updates on refreshButtons(): the count is read each time the icon is drawn
addButton(4, new InventoryButton()
    .creator(p -> named(Material.PLAYER_HEAD, "Online: " + Bukkit.getOnlinePlayers().size())));

// Doesn't update on refreshButtons(): the count was read once, in decorate
int online = Bukkit.getOnlinePlayers().size();
addButton(4, new InventoryButton()
    .creator(p -> named(Material.PLAYER_HEAD, "Online: " + online)));
```

To update on a timer, enable auto-refresh in the constructor. MenuAPI starts the task when the menu opens and stops it when the menu closes, when the player opens another menu, or when the player leaves:

```java
public WarpMenu(WarpManager warpManager, Plugin plugin) {
    this.warpManager = warpManager;
    enableAutoRefresh(plugin, 40L);   // every 2 seconds
}
```

Each tick calls `refreshGUI()`, which runs `refreshButtons()` by default. Override it to call `rebuild()` if the list itself can change:

```java
@Override
protected void refreshGUI() {
    rebuild();
}
```

To update a menu from outside it, for example when another player creates a warp, refresh every open menu of that type:

```java
GUIManager.getInstance().refreshAllGUIsOfType(WarpMenu.class);   // calls refreshGUI() on each
```

## Best practices

### Put buttons in `decorate`

Add every button in `decorate`, never in the constructor. A rebuild removes all buttons and then calls `decorate` again, so a button added in the constructor disappears after the first rebuild.

### Load data in `decorate` or `createInventory`

The constructor runs once, while `decorate` and `createInventory` run on every rebuild. Anything that can change while the menu is open, such as a list from a manager, the player's balance or a database result, should be read in one of those two methods.

If `createInventory` needs the data to choose the inventory size, read it there and keep the result in a field for `decorate` to use. Both run on every rebuild, `createInventory` first.

### Keep filtered results separate from the source

When a filter or search narrows a list, store the narrowed list in its own field and always build it from the full list:

```java
private WarpCategory category = WarpCategory.ALL;   // changed by a filter button
private List<Warp> shown;

@Override
protected Inventory createInventory() {
    shown = warpManager.getWarps().stream()
        .filter(warp -> category.includes(warp))
        .toList();
    int rows = Math.min(6, Math.max(2, (shown.size() + 8) / 9 + 1));
    return Bukkit.createInventory(null, rows * 9, "Warps");
}
```

Filtering a field in place (`warps = warps.filter(...)`) loses the entries the filter removed, so switching back to "All" can't bring them back.

### Constructor arguments are for what the menu needs, fields are for what it shows

Pass managers and other services to the constructor. Keep the page, filter and search query in fields that the menu changes itself. Change a field and call `rebuild()`; don't open a new instance of the same menu.

### Pass only what the next menu uses

A child menu needs its own services and data, and a reference to the menu Back goes to. It doesn't need the parent's services just so it can rebuild the parent. Call `previous.rebuild()` instead.

### Open each instance once

`openGUI` throws if you pass a menu that has already been opened. Use `rebuild()` to show the same menu again, or create a new instance for a fresh one.

### Let MenuAPI cancel clicks

Clicks are cancelled by default, so handlers don't need `event.setCancelled(true)`. Call `allowTopInventoryClick(true)` or `allowBottomInventoryClick(true)` in the constructor only for menus where players move items, such as a deposit box. Button handlers still run when clicks are allowed, and can cancel the click themselves.

### Don't close the inventory before opening another menu

`openGUI` replaces whatever menu the player has open, and the old menu's `onClose` runs as usual. Calling `closeInventory()` first only makes the screen flicker.

### Keep `creator` functions cheap

Creators run on every draw: when the menu opens, on every rebuild, and on every refresh. Do expensive work, such as database queries, in `decorate` or before opening the menu, and have the creator build the item from what was loaded.

### Stay on the main thread

`openGUI`, `rebuild()`, the refresh methods and everything on `GUIManager` must run on the server's main thread. MenuAPI tracks menus in plain maps and calls Bukkit inventory methods, and neither is safe from another thread. To show data from a database or web request, load it asynchronously, then switch back to open or rebuild:

```java
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    List<Warp> warps = warpStorage.loadAll();               // slow, off the main thread
    Bukkit.getScheduler().runTask(plugin, () -> {
        GUIManager.getInstance().openGUI(new WarpListMenu(warps), player);
    });
});
```

The same applies to a button that saves something: start the save asynchronously, and call `rebuild()` inside `runTask` when it finishes. The player may have closed the menu by then, and `rebuild()` opens a closed menu again. If that would surprise the player, check that the menu is still open first:

```java
Bukkit.getScheduler().runTask(plugin, () -> {
    if (GUIManager.getInstance().getOpenGUI(player) == this) {
        rebuild();
    }
});
```

For a player who has left, `rebuild()` does nothing.

### Use the player you're given

`decorate(Player player)` and `creator(p -> ...)` both receive the player the menu is open for. Use that, or `event.getWhoClicked()` in a handler. `getRegisteredPlayer()` doesn't work in the constructor.

### Name your slots

Use constants for fixed slots (`private static final int BACK_SLOT = 18;`). Layouts change, and a constant is one edit instead of a search.

## Common mistakes

| Symptom | Cause |
| --- | --- |
| Buttons do nothing, items can be taken | `GUIListener` isn't registered in `onEnable()` |
| `IllegalStateException` when opening a menu | The same instance was passed to `openGUI` twice. Call `rebuild()` instead |
| `IllegalStateException` from `getRegisteredPlayer()` | It was called in the constructor |
| Next page button does nothing visible | The handler calls `refreshButtons()`, which doesn't run `decorate` again. Call `rebuild()` |
| Buttons vanish after the first page change | They were added in the constructor. Move them to `decorate` |
| Data stays old after a rebuild | It's loaded in the constructor. Load it in `createInventory` or `decorate` |
| Clearing a filter doesn't bring entries back | The filtered list replaced the full list. Keep them in separate fields |
| `IllegalArgumentException` when the menu is drawn | A button is placed outside the inventory, for example slot 30 in a 27-slot menu |

## Next steps

- [API guide](MENUAPI_GUIDE.md): every feature, with more examples
- [Auto-refresh guide](AUTO_REFRESH_GUIDE.md)
- [Area buttons (IAButton)](IABUTTON_USAGE.md): one button covering a rectangle of slots
- [Managing open menus](GLOBAL_GUI_MANAGEMENT.md): finding and refreshing menus across players
- [ItemsAdder menus](MENUAPI_GUIDE.md#itemsadder-integration): textured menus and HUDs
