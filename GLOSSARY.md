# MenuAPI

A library for building interactive inventory menus in Bukkit/Paper plugins.

## Language

**GUI**:
A menu a player sees as a chest-style inventory, defined by subclassing `InventoryGUI`. One GUI instance is shown to one player. It is opened once, and after that only a rebuild shows it again.
_Avoid_: menu (in code), screen, window

**Button**:
An icon in one or more slots of a GUI, paired with what happens when it is clicked.
_Avoid_: item, slot handler

**Area button**:
A button that covers a rectangle of slots and behaves as one button.
_Avoid_: multi-slot button, IA button

**Session**:
The period from when a GUI is opened for a player until it closes. A player has at most one session at a time.
_Avoid_: open GUI entry, active inventory

**Rebuild**:
Building a GUI again from its current fields: its inventory is created again, `decorate` runs again and every button is replaced. An open GUI keeps its session; a closed one starts a new session.
_Avoid_: reopen, reload, recreate

**Auto-refresh**:
Redrawing a GUI on a fixed tick interval for as long as its session lasts.
_Avoid_: polling, live update

**Click rules**:
Whether clicks in the GUI's own inventory and in the player's inventory are cancelled by default.
_Avoid_: click control, click permissions
