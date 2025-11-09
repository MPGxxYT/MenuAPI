# Auto-Refresh System Guide

Complete guide to using MenuAPI's built-in auto-refresh system for dynamic, self-updating GUIs.

## Table of Contents
- [Overview](#overview)
- [Basic Usage](#basic-usage)
- [Configuration](#configuration)
- [Custom Refresh Behavior](#custom-refresh-behavior)
- [Advanced Examples](#advanced-examples)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

---

## Overview

The auto-refresh system automatically updates your GUI at regular intervals, perfect for:
- Displaying real-time player counts
- Showing live server statistics
- Updating countdown timers
- Refreshing dynamic content (lobby status, game state, etc.)

### Features
- **Automatic lifecycle management** - Starts when GUI opens, stops when closed
- **Player validation** - Auto-stops if player disconnects
- **IAButton support** - Multi-slot buttons refresh correctly
- **Customizable** - Override refresh behavior for your needs
- **Safe** - Prevents task leaks and duplicate refreshing

---

## Basic Usage

### Step 1: Enable Auto-Refresh

Enable auto-refresh in your GUI's constructor:

```java
public class DynamicMenu extends InventoryGUI {

  public DynamicMenu() {
    super();
    // Enable auto-refresh: update every 1 second (20 ticks)
    enableAutoRefresh(MyPlugin.getInstance(), 20L);
  }

  @Override
  protected Inventory createInventory() {
    return Bukkit.createInventory(null, 27, "Dynamic Menu");
  }

  @Override
  public void decorate(Player player) {
    addButton(13, dynamicButton());
    super.decorate(player);
  }

  private InventoryButton dynamicButton() {
    return new InventoryButton()
        .creator(player -> {
          // This will automatically refresh every second
          int onlinePlayers = Bukkit.getOnlinePlayers().size();
          ItemStack item = new ItemStack(Material.PLAYER_HEAD);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§e" + onlinePlayers + " Online");
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> event.setCancelled(true));
  }
}
```

That's it! The GUI will now auto-refresh every second.

---

## Configuration

### Setting Refresh Interval

The interval is specified in **ticks** (20 ticks = 1 second):

```java
// Refresh every 1 second (20 ticks)
enableAutoRefresh(plugin, 20L);

// Refresh every 2 seconds (40 ticks)
enableAutoRefresh(plugin, 40L);

// Refresh every 5 seconds (100 ticks)
enableAutoRefresh(plugin, 100L);

// Refresh every 30 seconds (600 ticks)
enableAutoRefresh(plugin, 600L);
```

### Disabling Auto-Refresh

```java
// Disable auto-refresh programmatically
disableAutoRefresh();

// Check if auto-refresh is enabled
if (isAutoRefreshEnabled()) {
  long interval = getAutoRefreshIntervalTicks();
  player.sendMessage("Refreshing every " + (interval / 20) + " seconds");
}
```

---

## Custom Refresh Behavior

Override `refreshGUI()` to customize what happens during each refresh cycle.

### Option 1: Simple Refresh (Default)

Just update button displays without recreating the inventory:

```java
@Override
protected void refreshGUI() {
  // This is the default behavior
  refreshButtons();
}
```

**Use when:**
- Button content changes but slots stay the same
- Number of items doesn't change
- Most efficient option

### Option 2: Full Refresh

Recreate the entire menu with updated data:

```java
@Override
protected void refreshGUI() {
  // Stop auto-refresh on this instance
  stopAutoRefresh();

  // Get updated data
  Set<Lobby> lobbies = LobbyManager.getInstance().getLobbies();

  // Open new menu instance (auto-refresh will restart)
  GUIManager.getInstance().openGUI(
      new LobbiesMenu(lobbies),
      getRegisteredPlayer()
  );
}
```

**Use when:**
- Number of items can change (items added/removed)
- Inventory size needs to change
- Complex data structure updates

### Option 3: Selective Refresh

Only refresh specific buttons:

```java
@Override
protected void refreshGUI() {
  // Only refresh specific slots
  refreshButton(10);  // Refresh player count
  refreshButton(13);  // Refresh server status
  refreshButton(16);  // Refresh timer
  // Other buttons remain unchanged
}
```

**Use when:**
- Only certain data changes
- Want to minimize processing
- Static decorative buttons don't need updating

---

## Advanced Examples

### Example 1: Game Lobby Menu with Live Updates

```java
public class LobbyMenu extends InventoryGUI {

  private final GameLobby lobby;

  public LobbyMenu(GameLobby lobby) {
    super();
    this.lobby = lobby;
    // Refresh every 0.5 seconds for responsive updates
    enableAutoRefresh(MyPlugin.getInstance(), 10L);
  }

  @Override
  protected Inventory createInventory() {
    return Bukkit.createInventory(null, 27, "Lobby: " + lobby.getName());
  }

  @Override
  public void decorate(Player player) {
    addButton(10, playerCountButton());
    addButton(13, statusButton());
    addButton(16, timerButton());
    addButton(22, joinButton());
    super.decorate(player);
  }

  private InventoryButton playerCountButton() {
    return new InventoryButton()
        .creator(player -> {
          int current = lobby.getPlayers().size();
          int max = lobby.getMaxPlayers();

          ItemStack item = new ItemStack(Material.PLAYER_HEAD);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§ePlayers: §f" + current + "/" + max);
          meta.setLore(Arrays.asList(
              lobby.getPlayers().stream()
                  .map(p -> "§7- " + p.getName())
                  .toArray(String[]::new)
          ));
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> event.setCancelled(true));
  }

  private InventoryButton statusButton() {
    return new InventoryButton()
        .creator(player -> {
          LobbyStatus status = lobby.getStatus();
          Material material = switch (status) {
            case WAITING -> Material.YELLOW_CONCRETE;
            case STARTING -> Material.ORANGE_CONCRETE;
            case IN_GAME -> Material.RED_CONCRETE;
            case ENDED -> Material.GRAY_CONCRETE;
          };

          ItemStack item = new ItemStack(material);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§eStatus: §f" + status.name());
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> event.setCancelled(true));
  }

  private InventoryButton timerButton() {
    return new InventoryButton()
        .creator(player -> {
          int countdown = lobby.getCountdown();

          ItemStack item = new ItemStack(Material.CLOCK);
          ItemMeta meta = item.getItemMeta();

          if (countdown > 0) {
            meta.setDisplayName("§eStarting in: §f" + countdown + "s");
          } else {
            meta.setDisplayName("§7Waiting for players...");
          }

          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> event.setCancelled(true));
  }

  private InventoryButton joinButton() {
    return new InventoryButton()
        .creator(player -> {
          boolean canJoin = lobby.canJoin(player);

          ItemStack item = new ItemStack(
              canJoin ? Material.LIME_DYE : Material.GRAY_DYE
          );
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName(canJoin ? "§a§lJoin Game" : "§c§lCannot Join");

          if (!canJoin) {
            meta.setLore(Arrays.asList("§cLobby is full or in progress"));
          }

          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> {
          event.setCancelled(true);
          Player player = (Player) event.getWhoClicked();

          if (lobby.canJoin(player)) {
            lobby.join(player);
            player.closeInventory();
          } else {
            player.sendMessage("§cYou cannot join this lobby!");
          }
        });
  }
}
```

### Example 2: Server Statistics Dashboard

```java
public class StatsDashboard extends InventoryGUI {

  public StatsDashboard() {
    super();
    // Update every 2 seconds
    enableAutoRefresh(MyPlugin.getInstance(), 40L);
  }

  @Override
  protected Inventory createInventory() {
    return Bukkit.createInventory(null, 27, "§6§lServer Stats");
  }

  @Override
  public void decorate(Player player) {
    addButton(10, tpsButton());
    addButton(12, memoryButton());
    addButton(14, playersButton());
    addButton(16, uptimeButton());
    super.decorate(player);
  }

  private InventoryButton tpsButton() {
    return new InventoryButton()
        .creator(player -> {
          double tps = ServerUtil.getTPS();
          String color = tps > 18 ? "§a" : tps > 15 ? "§e" : "§c";

          ItemStack item = new ItemStack(Material.REDSTONE);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§eTPS: " + color + String.format("%.2f", tps));
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> event.setCancelled(true));
  }

  private InventoryButton memoryButton() {
    return new InventoryButton()
        .creator(player -> {
          Runtime runtime = Runtime.getRuntime();
          long used = (runtime.totalMemory() - runtime.freeMemory()) / 1048576;
          long max = runtime.maxMemory() / 1048576;
          double percent = (used / (double) max) * 100;

          String color = percent < 70 ? "§a" : percent < 90 ? "§e" : "§c";

          ItemStack item = new ItemStack(Material.EMERALD);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§eMemory: " + color + used + "MB§7/§f" + max + "MB");
          meta.setLore(Arrays.asList("§7Usage: " + color + String.format("%.1f%%", percent)));
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> event.setCancelled(true));
  }

  private InventoryButton playersButton() {
    return new InventoryButton()
        .creator(player -> {
          int online = Bukkit.getOnlinePlayers().size();
          int max = Bukkit.getMaxPlayers();

          ItemStack item = new ItemStack(Material.PLAYER_HEAD);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§ePlayers: §f" + online + "/" + max);
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> event.setCancelled(true));
  }

  private InventoryButton uptimeButton() {
    return new InventoryButton()
        .creator(player -> {
          long uptime = ServerUtil.getUptime();
          String formatted = ServerUtil.formatDuration(uptime);

          ItemStack item = new ItemStack(Material.CLOCK);
          ItemMeta meta = item.getItemMeta();
          meta.setDisplayName("§eUptime: §f" + formatted);
          item.setItemMeta(meta);
          return item;
        })
        .consumer(event -> event.setCancelled(true));
  }
}
```

### Example 3: Conditional Auto-Refresh

Enable auto-refresh only under certain conditions:

```java
public class ConditionalMenu extends InventoryGUI {

  private final boolean needsRefresh;

  public ConditionalMenu(boolean needsRefresh) {
    super();
    this.needsRefresh = needsRefresh;

    // Only enable auto-refresh if needed
    if (needsRefresh) {
      enableAutoRefresh(MyPlugin.getInstance(), 20L);
    }
  }

  @Override
  protected void refreshGUI() {
    // You can also disable auto-refresh dynamically
    if (someConditionMet()) {
      disableAutoRefresh();
    } else {
      refreshButtons();
    }
  }
}
```

---

## Best Practices

### 1. Choose Appropriate Refresh Intervals

```java
// Fast-changing data (countdown timers, live stats)
enableAutoRefresh(plugin, 10L);  // 0.5 seconds

// Moderate updates (player counts, lobby status)
enableAutoRefresh(plugin, 40L);  // 2 seconds

// Slow updates (leaderboards, statistics)
enableAutoRefresh(plugin, 100L); // 5 seconds
```

**Don't refresh too frequently!** It wastes server resources and creates visual noise.

### 2. Use Simple Refresh When Possible

```java
@Override
protected void refreshGUI() {
  // Prefer this (efficient)
  refreshButtons();

  // Over this (expensive)
  stopAutoRefresh();
  GUIManager.getInstance().openGUI(new MyMenu(), getRegisteredPlayer());
}
```

Only use full refresh when the number of items or inventory size changes.

### 3. Stop Refresh When Not Needed

```java
@Override
protected void refreshGUI() {
  if (dataIsStatic()) {
    disableAutoRefresh();
    return;
  }

  refreshButtons();
}
```

### 4. Cache Expensive Calculations

```java
private int cachedPlayerCount = 0;
private long lastUpdate = 0;

private InventoryButton playerCountButton() {
  return new InventoryButton()
      .creator(player -> {
        // Update cache every 5 seconds, even if GUI refreshes more often
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 5000) {
          cachedPlayerCount = ExpensiveCalculation.getPlayerCount();
          lastUpdate = now;
        }

        // Use cached value
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + cachedPlayerCount + " Players");
        item.setItemMeta(meta);
        return item;
      })
      .consumer(event -> event.setCancelled(true));
}
```

### 5. Consider Using refreshButton() for Selective Updates

```java
// Instead of refreshing everything
refreshButtons();

// Refresh only what changed
refreshButton(10);  // Only slot 10 updates
```

---

## Troubleshooting

### Auto-Refresh Not Starting

**Problem:** GUI opens but doesn't auto-refresh.

**Solutions:**
- Ensure you called `enableAutoRefresh()` in the constructor
- Check that the plugin instance is not null
- Verify the interval is positive (> 0)
- Make sure `super.onOpen()` is called if you override `onOpen()`

```java
public MyMenu() {
  super();
  enableAutoRefresh(MyPlugin.getInstance(), 20L);  // Must be in constructor
}
```

### Auto-Refresh Not Stopping

**Problem:** Task continues after GUI closes.

**Solutions:**
- Don't call `startAutoRefresh()` manually
- Ensure `super.onClose()` is called if you override `onClose()`
- Check for duplicate GUI instances

```java
@Override
public void onClose(InventoryCloseEvent event) {
  super.onClose(event);  // This stops auto-refresh
  // Your custom close logic here
}
```

### High Server Load

**Problem:** Auto-refresh causing lag.

**Solutions:**
- Increase refresh interval (use larger tick values)
- Cache expensive calculations
- Use `refreshButton()` instead of `refreshButtons()`
- Disable auto-refresh when data is static

```java
// Too frequent - avoid
enableAutoRefresh(plugin, 1L);  // Every tick!

// Better
enableAutoRefresh(plugin, 20L); // Every second

// Best for most cases
enableAutoRefresh(plugin, 40L); // Every 2 seconds
```

### Items Flickering

**Problem:** Items visually flicker during refresh.

**Solutions:**
- Increase refresh interval
- Only refresh buttons that actually changed
- Use `refreshButton()` for targeted updates

```java
@Override
protected void refreshGUI() {
  // Only refresh dynamic content
  refreshButton(10);  // Player count
  refreshButton(13);  // Status
  // Leave static buttons alone
}
```

### Memory Leaks

**Problem:** Memory usage grows over time.

**Solutions:**
- Always stop tasks in `onClose()`
- Don't create new GUI instances in `refreshGUI()` unless necessary
- Clear references to old data

```java
@Override
protected void refreshGUI() {
  // Bad - creates memory leak
  new Timer().schedule(new TimerTask() {
    public void run() {
      refreshButtons();
    }
  }, 1000);

  // Good - uses built-in system
  refreshButtons();
}
```

---

## Summary

Auto-refresh system provides:
- ✅ Automatic updates at regular intervals
- ✅ Safe lifecycle management
- ✅ Customizable refresh behavior
- ✅ IAButton support
- ✅ Player validation
- ✅ Resource-efficient

For more information, see:
- [Main MenuAPI Guide](MENUAPI_GUIDE.md)
- [Global GUI Management Guide](GLOBAL_GUI_MANAGEMENT.md)
- [IAButton Usage Guide](IABUTTON_USAGE.md)
