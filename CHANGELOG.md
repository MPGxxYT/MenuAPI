# Changelog

Changes to MenuAPI, newest first. Versions match the Git tags that JitPack builds from.

## 2.1.0

### Added

- `InventoryGUI.rebuild()` builds a menu again from its own fields. `createInventory()` and `decorate()` run again on the same instance, and every button is replaced by the ones `decorate()` adds. A menu that changes a page, filter or search no longer has to open a new instance of itself, so managers passed to its constructor don't need to be passed again.
  - Open menu, same size: redrawn in the inventory on screen. The cursor stays where it is.
  - Open menu, new size: the new inventory opens and the session continues. `onOpen` and `onClose` are not called.
  - Closed menu, for example after an anvil prompt: opens again as a new session, and `onOpen` is called. Any other menu the player has open is replaced.
  - Offline player: nothing happens.
  - Auto-refresh keeps running through a rebuild, and restarts if the rebuild changed its interval or turned it on or off.
  - `ItemsAdderInventoryGUI`: a menu that wraps an existing inventory redraws its buttons in place. A menu that overrides `createItemsAdderWrapper` gets a new wrapper, shown again.
- [Getting Started](docs/GETTING_STARTED.md) guide: a first menu built step by step, with best practices.
- Shading and relocation instructions in the README.

### Notes

- Add buttons in `decorate()`, not the constructor. A rebuild removes every button before calling `decorate()` again.
- The constructor does not run again on a rebuild. Load data that must stay current in `createInventory()` or `decorate()`.
- A title change alone is not shown when the size stays the same, since Paper 1.19.4 can't rename an open inventory.
- `rebuild()` throws `IllegalStateException` if the menu was never opened with `openGUI`, or if it's called from inside `createInventory()` or `decorate()`.
- `openGUI` still throws on a second open of the same instance.

### Fixed (docs)

- The global GUI management guide said tracking was thread-safe. It isn't: `GUIManager` must be used from the main thread.
- The API guide's paged example called `refreshButtons()` to change pages, which doesn't run `decorate()` again. It calls `rebuild()` now.
- The API guide told users to cancel clicks and close the inventory before opening another menu. MenuAPI does both already.

No breaking changes.

## 2.0.0

`GUIManager` now runs every menu session: opening, drawing buttons, click rules, closing and auto-refresh. Menus only describe their inventory and buttons.

### Breaking changes

- `decorate`, `onOpen`, `onClick` and `onClose` are optional hooks. Calls to `super` are no longer needed, and existing ones do nothing.
- `InventoryGUI.getOpenGUI`, `hasOpenGUI`, `refreshAllGUIsOfType` and `refreshButtonsForType` are now instance methods on `GUIManager.getInstance()`.
- `startAutoRefresh` and `stopAutoRefresh` are removed. Use `disableAutoRefresh()` to stop refreshing while a menu stays open.
- `InventoryHandler`, `registerHandledInventory` and `unregisterInventory` are removed. Open every menu with `GUIManager.getInstance().openGUI(gui, player)`.
- `GUIManager.handleOpen` is removed, and `onOpen` can no longer cancel the open. It runs after the menu is on screen and its buttons are drawn.
- `getButtonMap()` is replaced by `getButton(slot)` and `removeButton(slot)`.
- A menu instance can be opened once. Opening it a second time throws `IllegalStateException`.
- An `IAButton` that runs past the right edge of the inventory throws `IllegalArgumentException` instead of being clipped.
- An ItemsAdder wrapper whose `showInventory` throws without opening anything makes `openGUI` throw `IllegalStateException`.

### Changed

- Clicks run in this order: click rules, then `onClick`, then the button's handler.
- If another plugin cancels the open, no session starts.
- A button covering several slots has its icon created once per draw and copied into each slot.
- A button placed outside the inventory throws `IllegalArgumentException` when the menu is drawn.
- `ItemsAdderInventoryGUI` reuses an inventory that's already on screen instead of opening it again.

## 1.0.0

First tagged release.
