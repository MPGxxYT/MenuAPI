# Global GUI Management Guide

Complete guide to tracking and managing GUIs across all players using MenuAPI's static methods.

## Table of Contents
- [Overview](#overview)
- [Tracking Open GUIs](#tracking-open-guis)
- [Global Refresh Methods](#global-refresh-methods)
- [Use Cases](#use-cases)
- [Advanced Examples](#advanced-examples)
- [Best Practices](#best-practices)

---

## Overview

MenuAPI automatically tracks all open GUIs across all players, allowing you to:
- Refresh all instances of a specific GUI type
- Check if a player has a GUI open
- Get a player's current GUI
- Update multiple players' GUIs when data changes

### Key Features
- **Automatic tracking** - GUIs register on open, unregister on close
- **Type-safe** - Refresh specific GUI classes
- **Thread-safe** - Uses ConcurrentHashMap
- **No manual management** - Everything is automatic

---

## Tracking Open GUIs

MenuAPI automatically tracks which GUI each player has open.

### Check if Player Has GUI Open

```java
Player player = /* ... */;

if (InventoryGUI.hasOpenGUI(player)) {
  player.sendMessage("§cYou already have a menu open!");
  return;
}

// Safe to open new GUI
GUIManager.getInstance().openGUI(new MyMenu(), player);
```

### Get Player's Current GUI

```java
Player player = /* ... */;
InventoryGUI gui = InventoryGUI.getOpenGUI(player);

if (gui == null) {
  player.sendMessage("§cYou don't have any menu open!");
  return;
}

// Check GUI type
if (gui instanceof PlayMenu) {
  player.sendMessage("§aYou're viewing the play menu!");
} else if (gui instanceof SettingsMenu) {
  player.sendMessage("§aYou're viewing the settings menu!");
}
```

### Type-Safe GUI Retrieval

```java
InventoryGUI gui = InventoryGUI.getOpenGUI(player);

if (gui instanceof LobbyMenu) {
  LobbyMenu lobbyMenu = (LobbyMenu) gui;
  Lobby lobby = lobbyMenu.getLobby();
  player.sendMessage("§aYou're viewing: " + lobby.getName());
}
```

---

## Global Refresh Methods

Update all open instances of a specific GUI type across all players.

### Full Refresh All GUIs

Calls `refreshGUI()` on all matching GUI instances:

```java
// Refresh all open LobbiesMenu instances
InventoryGUI.refreshAllGUIsOfType(LobbiesMenu.class);
```

**Use when:**
- Data structure changed (items added/removed)
- Need complete GUI recreation
- Complex updates required

### Button Refresh All GUIs

More efficient - only updates button displays:

```java
// Just update button displays on all PlayMenu instances
InventoryGUI.refreshButtonsForType(PlayMenu.class);
```

**Use when:**
- Button content changed but structure stayed same
- Number of items hasn't changed
- Want maximum efficiency

---

## Use Cases

### Use Case 1: Data Manager Integration

Automatically refresh GUIs when managed data changes:

```java
public class LobbyManager {

  private final Set<Lobby> lobbies = new HashSet<>();

  public void createLobby(String name) {
    Lobby lobby = new Lobby(name);
    lobbies.add(lobby);

    // Refresh all open lobby list menus
    InventoryGUI.refreshAllGUIsOfType(LobbiesMenu.class);
  }

  public void deleteLobby(Lobby lobby) {
    lobbies.remove(lobby);

    // Refresh all open lobby list menus
    InventoryGUI.refreshAllGUIsOfType(LobbiesMenu.class);
  }

  public void updateLobby(Lobby lobby) {
    // Just update displays, structure hasn't changed
    InventoryGUI.refreshButtonsForType(LobbiesMenu.class);
  }
}
```

### Use Case 2: Player Join/Leave Events

Update player-related GUIs when players join or leave:

```java
@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
  // Update all play menus to show new player count
  InventoryGUI.refreshButtonsForType(PlayMenu.class);

  // Update all lobby menus
  InventoryGUI.refreshButtonsForType(LobbyMenu.class);
}

@EventHandler
public void onPlayerQuit(PlayerQuitEvent event) {
  // Update all play menus to show new player count
  InventoryGUI.refreshButtonsForType(PlayMenu.class);

  // Update all lobby menus
  InventoryGUI.refreshButtonsForType(LobbyMenu.class);
}
```

### Use Case 3: Game State Changes

Update GUIs when game state changes:

```java
public class Game {

  public void start() {
    this.status = GameStatus.IN_PROGRESS;

    // Refresh all lobby browsers to show game started
    InventoryGUI.refreshButtonsForType(GameBrowserMenu.class);

    // Update spectator menus
    InventoryGUI.refreshButtonsForType(SpectateMenu.class);
  }

  public void end() {
    this.status = GameStatus.ENDED;

    // Full refresh needed - player list changed
    InventoryGUI.refreshAllGUIsOfType(GameBrowserMenu.class);
  }
}
```

### Use Case 4: Permission Changes

Update GUIs when player permissions change:

```java
public void grantPermission(Player player, String permission) {
  player.addAttachment(plugin).setPermission(permission, true);

  // Check if this player has a menu open
  InventoryGUI gui = InventoryGUI.getOpenGUI(player);

  if (gui != null) {
    // Refresh their specific GUI
    gui.refreshButtons();
    player.sendMessage("§aMenu updated with new permissions!");
  }
}
```

### Use Case 5: Preventing Duplicate GUIs

Prevent players from opening multiple GUIs:

```java
public void openPlayMenu(Player player) {
  // Check if player already has a GUI open
  if (InventoryGUI.hasOpenGUI(player)) {
    InventoryGUI current = InventoryGUI.getOpenGUI(player);

    if (current instanceof PlayMenu) {
      player.sendMessage("§cYou already have the play menu open!");
      return;
    }

    // Close current GUI before opening new one
    player.closeInventory();
  }

  // Safe to open
  GUIManager.getInstance().openGUI(new PlayMenu(), player);
}
```

---

## Advanced Examples

### Example 1: Selective Refresh Based on Data

```java
public class Shop {

  private final Map<String, ShopItem> items = new HashMap<>();

  public void updateItem(String itemId, ShopItem newItem) {
    items.put(itemId, newItem);

    // Only refresh shop menus, not other GUIs
    InventoryGUI.refreshButtonsForType(ShopMenu.class);

    // Also update player's personal shop if they have it open
    for (Player player : Bukkit.getOnlinePlayers()) {
      InventoryGUI gui = InventoryGUI.getOpenGUI(player);

      if (gui instanceof PersonalShopMenu) {
        PersonalShopMenu shop = (PersonalShopMenu) gui;

        // Only refresh if they're viewing this specific item
        if (shop.isViewingItem(itemId)) {
          shop.refreshButtons();
        }
      }
    }
  }
}
```

### Example 2: Bulk Updates

```java
public class BulkDataUpdater {

  public void updateAllData() {
    // Update multiple data sources
    updateLobbies();
    updateGames();
    updatePlayers();

    // Single refresh at the end instead of multiple refreshes
    InventoryGUI.refreshAllGUIsOfType(DashboardMenu.class);
    InventoryGUI.refreshButtonsForType(LobbiesMenu.class);
    InventoryGUI.refreshButtonsForType(GamesMenu.class);
  }
}
```

### Example 3: Conditional Refresh

```java
public class RankManager {

  public void updateRank(Player player, Rank newRank) {
    player.setRank(newRank);

    // Refresh menus that display ranks
    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      InventoryGUI gui = InventoryGUI.getOpenGUI(onlinePlayer);

      if (gui instanceof PlayerListMenu) {
        // Only refresh if they're viewing the updated player
        PlayerListMenu menu = (PlayerListMenu) gui;
        if (menu.isShowingPlayer(player)) {
          menu.refreshButtons();
        }
      } else if (gui instanceof LeaderboardMenu) {
        // Always refresh leaderboards
        gui.refreshButtons();
      }
    }
  }
}
```

### Example 4: Targeted Notifications

Notify players when their open GUI has been updated:

```java
public class NotificationManager {

  public void updateAndNotify(Class<? extends InventoryGUI> guiClass, String message) {
    // Track which players were affected
    Set<Player> affectedPlayers = new HashSet<>();

    for (Player player : Bukkit.getOnlinePlayers()) {
      InventoryGUI gui = InventoryGUI.getOpenGUI(player);

      if (gui != null && guiClass.isInstance(gui)) {
        affectedPlayers.add(player);
      }
    }

    // Refresh the GUIs
    InventoryGUI.refreshButtonsForType(guiClass);

    // Notify affected players
    for (Player player : affectedPlayers) {
      player.sendMessage(message);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
    }
  }
}

// Usage
notificationManager.updateAndNotify(
    ShopMenu.class,
    "§e⚠ Shop inventory has been updated!"
);
```

### Example 5: GUI State Synchronization

Keep multiple GUI types synchronized:

```java
public class SyncManager {

  public void syncAllGameMenus() {
    // Refresh all game-related GUIs together
    InventoryGUI.refreshButtonsForType(GameBrowserMenu.class);
    InventoryGUI.refreshButtonsForType(LobbyMenu.class);
    InventoryGUI.refreshButtonsForType(QueueMenu.class);
    InventoryGUI.refreshButtonsForType(SpectateMenu.class);
  }

  public void onGameStart(Game game) {
    syncAllGameMenus();

    // Notify players viewing game menus
    for (Player player : Bukkit.getOnlinePlayers()) {
      InventoryGUI gui = InventoryGUI.getOpenGUI(player);

      if (gui instanceof GameBrowserMenu) {
        player.sendMessage("§aGame " + game.getName() + " has started!");
      }
    }
  }
}
```

---

## Best Practices

### 1. Use Button Refresh When Possible

```java
// Prefer this (efficient)
InventoryGUI.refreshButtonsForType(MyMenu.class);

// Over this (expensive)
InventoryGUI.refreshAllGUIsOfType(MyMenu.class);
```

Only use `refreshAllGUIsOfType()` when the structure changes (items added/removed).

### 2. Batch Updates

```java
// Bad - multiple refreshes
updateData1();
InventoryGUI.refreshButtonsForType(MyMenu.class);
updateData2();
InventoryGUI.refreshButtonsForType(MyMenu.class);
updateData3();
InventoryGUI.refreshButtonsForType(MyMenu.class);

// Good - single refresh
updateData1();
updateData2();
updateData3();
InventoryGUI.refreshButtonsForType(MyMenu.class);
```

### 3. Check Before Refreshing

```java
// Avoid unnecessary refreshes
public void updateLobby(Lobby lobby) {
  lobby.setStatus(LobbyStatus.IN_GAME);

  // Only refresh if someone is actually viewing
  boolean hasViewers = false;
  for (Player player : Bukkit.getOnlinePlayers()) {
    if (InventoryGUI.getOpenGUI(player) instanceof LobbiesMenu) {
      hasViewers = true;
      break;
    }
  }

  if (hasViewers) {
    InventoryGUI.refreshButtonsForType(LobbiesMenu.class);
  }
}
```

### 4. Use Specific GUI Types

```java
// Good - specific types
InventoryGUI.refreshButtonsForType(LobbiesMenu.class);
InventoryGUI.refreshButtonsForType(PlayMenu.class);

// Bad - refreshing base class
InventoryGUI.refreshButtonsForType(InventoryGUI.class); // Refreshes ALL GUIs!
```

### 5. Handle Null GUIs

```java
InventoryGUI gui = InventoryGUI.getOpenGUI(player);

// Always check for null
if (gui == null) {
  player.sendMessage("§cYou don't have a menu open!");
  return;
}

// Safe to use
if (gui instanceof MyMenu) {
  // Do something
}
```

### 6. Combine with Auto-Refresh

```java
public class SmartMenu extends InventoryGUI {

  public SmartMenu() {
    super();
    // Auto-refresh for this instance
    enableAutoRefresh(MyPlugin.getInstance(), 40L);
  }

  // Also support global refresh
  public static void refreshAll() {
    InventoryGUI.refreshButtonsForType(SmartMenu.class);
  }
}

// Individual menus refresh every 2 seconds
// Can also manually refresh all at once
SmartMenu.refreshAll();
```

---

## API Reference

### Static Methods

```java
// Refresh all GUIs of a type (calls refreshGUI())
InventoryGUI.refreshAllGUIsOfType(Class<? extends InventoryGUI> guiClass)

// Refresh buttons only (calls refreshButtons())
InventoryGUI.refreshButtonsForType(Class<? extends InventoryGUI> guiClass)

// Get player's open GUI
InventoryGUI.getOpenGUI(Player player)  // Returns null if none open

// Check if player has GUI open
InventoryGUI.hasOpenGUI(Player player)  // Returns boolean
```

### When to Use Each Method

| Method | When to Use | Performance |
|--------|-------------|-------------|
| `refreshButtonsForType()` | Button content changed, structure same | ⚡ Fast |
| `refreshAllGUIsOfType()` | Items added/removed, size changed | 🐌 Slower |
| `getOpenGUI()` | Need specific GUI instance | ⚡ Fast |
| `hasOpenGUI()` | Just checking if GUI open | ⚡ Fast |

---

## Troubleshooting

### GUIs Not Refreshing

**Problem:** Called refresh method but GUIs didn't update.

**Solutions:**
- Verify the correct GUI class is being passed
- Check that players actually have that GUI open
- Ensure button creators use current data (not cached)
- Call from main thread (use `Bukkit.getScheduler().runTask()` if async)

```java
// If calling from async context
Bukkit.getScheduler().runTask(plugin, () -> {
  InventoryGUI.refreshButtonsForType(MyMenu.class);
});
```

### Performance Issues

**Problem:** Refreshing causes lag.

**Solutions:**
- Use `refreshButtonsForType()` instead of `refreshAllGUIsOfType()`
- Don't refresh too frequently
- Batch updates together
- Cache expensive calculations

### Wrong GUIs Being Refreshed

**Problem:** Unintended GUIs are being refreshed.

**Solutions:**
- Use specific GUI classes, not base classes
- Check inheritance hierarchy
- Use `instanceof` checks for safety

```java
// Too broad - refreshes ALL GUIs
InventoryGUI.refreshButtonsForType(InventoryGUI.class);

// Specific - only refreshes PlayMenu
InventoryGUI.refreshButtonsForType(PlayMenu.class);
```

---

## Summary

Global GUI management provides:
- ✅ Automatic tracking of all open GUIs
- ✅ Type-safe refresh methods
- ✅ Thread-safe concurrent access
- ✅ No manual registration required
- ✅ Easy integration with data managers

For more information, see:
- [Main MenuAPI Guide](MENUAPI_GUIDE.md)
- [Auto-Refresh Guide](AUTO_REFRESH_GUIDE.md)
- [IAButton Usage Guide](IABUTTON_USAGE.md)
