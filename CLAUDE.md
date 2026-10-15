# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

MenuAPI is a library (not a standalone plugin, so there is no `plugin.yml`) for building inventory GUIs in Bukkit/Paper plugins. Consumers pull it in through JitPack (`com.github.MPGxxYT:MenuAPI`) and register `GUIListener` themselves in `onEnable()`. Java 21, compiled against `paper-api` 1.19.4 (scope `provided`). All sources are in the package `me.mortaldev.menuapi`.

## Build

```bash
mvn package
```

Tests use JUnit 5 and MockBukkit-v1.19 (matches `paper-api 1.19.4`). Run one with `mvn test -Dtest=GUIManagerTest#methodName`. `TESTING.md` lists further planned tests.

Formatting follows google-java-format (enabled in `.idea/google-java-format.xml`), 2-space indentation.

## Architecture

Event flow: Bukkit event -> `GUIListener` -> `GUIManager` (singleton; package-private constructor for tests) -> the `InventoryGUI` registered for that `Inventory` instance. Domain terms are in `GLOSSARY.md`.

- `GUIManager` owns every session: a `Map<Inventory, InventoryGUI>` for event routing and a UUID-keyed map of sessions (GUI plus its refresh task). `openGUI` calls the GUI's package-private `show(Player)`, checks that the returned inventory is on screen (no session if the open was cancelled), then calls `decorate`, draws the buttons, calls the `onOpen` hook with an event it builds, and starts auto-refresh. Bukkit's own open event is not used. On click it applies the click rules, then `onClick`, then the button handler. On close it calls `onClose`, then `hidden`, stops the task and drops the session. It also exposes `getOpenGUI`, `hasOpenGUI`, `refreshAllGUIsOfType` and `refreshButtonsForType`.
- `InventoryGUI` (abstract): subclasses implement `createInventory()` and add buttons in `decorate(Player)`. `decorate`, `onOpen`, `onClick` and `onClose` are hooks with empty defaults, so subclasses never need `super`. Package-private methods (`registerPlayer`, `show`, `hidden`, `createRebuildInventory`, `renderButtons`, `applyClickRules`, `clickButton`) are called only by `GUIManager`. `openGUI` throws on a second open of the same instance; after that, `rebuild()` (delegating to `GUIManager.rebuild`) runs `createInventory` and `decorate` again on the same instance. Same size redraws into the inventory on screen, a new size opens the new inventory with the session kept, and a closed GUI reopens as a new session. `rebuild()` throws if called while `createInventory` or `decorate` is running.
- Buttons: `InventoryButton` is a fluent pair of `creator` (`Function<Player, ItemStack>`) and `consumer` (`Consumer<InventoryClickEvent>`). `IAButton` extends it with `size`/`anchor`. `InventoryGUI` delegates placement and drawing to the package-private `ButtonLayout`, which maps slots to buttons, checks slots against the inventory size before drawing, calls each creator once per draw, and redraws or removes a button in every slot it covers.
- Auto-refresh: the GUI declares it with `enableAutoRefresh(plugin, ticks)` before opening; `GUIManager` runs the `BukkitTask`, which calls the overridable `refreshGUI()` (defaults to `refreshButtons()`) and stops on close, when another GUI opens for that player, when the player goes offline, or after `disableAutoRefresh()`.
- ItemsAdder: there is no compile-time dependency. `ItemsAdderInventoryGUI` either wraps an existing `Inventory` or overrides `createItemsAdderWrapper(Player)` to return a `TexturedInventoryWrapper` as `Object`. It overrides `show`/`hidden`: `show` calls `showInventory` through reflection (or reuses an inventory already on screen) and shows HUDs, `hidden` hides them. `GUIManager` has no ItemsAdder code. Keep ItemsAdder access reflective so the library still loads without ItemsAdder installed.

## Docs

`README.md` and `docs/*.md` (API guide, auto-refresh, IAButton, global GUI management) are user-facing and contain code examples. Update them when public API behavior changes.
