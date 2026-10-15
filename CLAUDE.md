# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

MenuAPI is a library (not a standalone plugin, so there is no `plugin.yml`) for building inventory GUIs in Bukkit/Paper plugins. Consumers pull it in through JitPack (`com.github.MPGxxYT:MenuAPI`) and register `GUIListener` themselves in `onEnable()`. Java 21, compiled against `paper-api` 1.19.4 (scope `provided`). All sources are in the package `me.mortaldev.menuapi`.

## Build

```bash
mvn package
```

Tests use JUnit 5 and MockBukkit-v1.19 (matches `paper-api 1.19.4`). Run one with `mvn test -Dtest=GUIManagerTest#methodName`. `TESTING.md` lists further planned tests.

Formatting follows google-java-format (enabled in `.idea/google-java-format.xml`). Note that `InventoryButton`, `IAButton` and `GUIListener` still use 4-space indentation while the other classes use 2-space; match the file you are editing.

## Architecture

Event flow: Bukkit event -> `GUIListener` -> `GUIManager` (singleton; package-private constructor for tests) -> the `InventoryGUI` registered for that `Inventory` instance. Domain terms are in `GLOSSARY.md`.

- `GUIManager` owns every session: a `Map<Inventory, InventoryGUI>` for event routing and a UUID-keyed map of sessions (GUI plus its refresh task). On open it calls `decorate`, draws the buttons, calls the `onOpen` hook and starts auto-refresh. On click it applies the click rules, then `onClick`, then the button handler. On close it calls `onClose`, stops the task and drops the session. It also exposes `getOpenGUI`, `hasOpenGUI`, `refreshAllGUIsOfType` and `refreshButtonsForType`.
- `InventoryGUI` (abstract): subclasses implement `createInventory()` and add buttons in `decorate(Player)`. `decorate`, `onOpen`, `onClick` and `onClose` are hooks with empty defaults, so subclasses never need `super`. Package-private methods (`registerPlayer`, `renderButtons`, `applyClickRules`, `clickButton`) are called only by `GUIManager`. A GUI instance is single-use; `openGUI` throws on a second open.
- Buttons: `InventoryButton` is a fluent pair of `creator` (`Function<Player, ItemStack>`) and `consumer` (`Consumer<InventoryClickEvent>`). `IAButton` extends it with `size`/`anchor` and is stored in `buttonMap` under every slot from `getOccupiedSlots()`, so `refreshButton(slot)` on any of its slots repaints the whole area.
- Auto-refresh: the GUI declares it with `enableAutoRefresh(plugin, ticks)` before opening; `GUIManager` runs the `BukkitTask`, which calls the overridable `refreshGUI()` (defaults to `refreshButtons()`) and stops on close, when another GUI opens for that player, when the player goes offline, or after `disableAutoRefresh()`.
- ItemsAdder: there is no compile-time dependency. `ItemsAdderInventoryGUI` either wraps an existing `Inventory` or overrides `createItemsAdderWrapper(Player)` to return a `TexturedInventoryWrapper` as `Object`. `GUIManager.openGUI` special-cases it, calls `showInventory` and the HUD show/hide APIs through reflection, takes the opened top inventory, and starts the session directly. An ItemsAdder GUI built around an inventory that is already open starts its session without reopening it. Keep ItemsAdder access reflective so the library still loads without ItemsAdder installed.

## Docs

`README.md` and `docs/*.md` (API guide, auto-refresh, IAButton, global GUI management) are user-facing and contain code examples. Update them when public API behavior changes.
