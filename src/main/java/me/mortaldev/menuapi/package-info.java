/**
 * MenuAPI: clickable inventory menus for Paper plugins.
 *
 * <p>Getting started:
 *
 * <ol>
 *   <li>Register a {@link me.mortaldev.menuapi.GUIListener} once, in your plugin's {@code
 *       onEnable()}: {@code getServer().getPluginManager().registerEvents(new
 *       GUIListener(GUIManager.getInstance()), this);}
 *   <li>Extend {@link me.mortaldev.menuapi.InventoryGUI}. Return the inventory to show from {@code
 *       createInventory()}, and add {@link me.mortaldev.menuapi.InventoryButton}s in {@code
 *       decorate(Player)}.
 *   <li>Open it with {@code GUIManager.getInstance().openGUI(new MyMenu(), player)}.
 *   <li>When a click changes what the menu shows, change the menu's fields and call {@link
 *       me.mortaldev.menuapi.InventoryGUI#rebuild()}.
 * </ol>
 *
 * <p>The classes:
 *
 * <ul>
 *   <li>{@link me.mortaldev.menuapi.InventoryGUI}: the base class for a menu.
 *   <li>{@link me.mortaldev.menuapi.InventoryButton}: an icon and a click handler in one slot or
 *       several.
 *   <li>{@link me.mortaldev.menuapi.IAButton}: a button that covers a rectangle of slots.
 *   <li>{@link me.mortaldev.menuapi.GUIManager}: opens menus, finds open ones, and refreshes them
 *       across players.
 *   <li>{@link me.mortaldev.menuapi.GUIListener}: passes Bukkit's click and close events to the
 *       manager.
 *   <li>{@link me.mortaldev.menuapi.ItemsAdderInventoryGUI}: a menu with an ItemsAdder textured
 *       inventory and HUDs.
 * </ul>
 *
 * <p>Everything in this package must be used from the server's main thread. The guides in the
 * repository's {@code docs} folder cover each part in more detail, starting with {@code
 * docs/GETTING_STARTED.md}.
 */
package me.mortaldev.menuapi;
