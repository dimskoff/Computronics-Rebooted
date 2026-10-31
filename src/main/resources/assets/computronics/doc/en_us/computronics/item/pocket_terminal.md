# Pocket Terminal

Handheld wireless terminal and pager displaying real-time data directly on the player HUD overlay.

## Features
* **Power Toggle:** Right-click while holding in hand toggles power ON / OFF.
* **Energy Capacity:** 20,000 EU. Recharges automatically when standing on an Inductive Charger Pad.
* **Resolution:** 30 columns by 10 rows.
* **Wireless Protocol:** Standard OpenComputers wireless packets on channel `404`.

## Lua Network API
```lua
local component = require("component")
local m = component.modem
m.open(404)

m.broadcast(404, "pt", termId, "clear")
m.broadcast(404, "pt", termId, "set", 1, 1, "SYSTEM ONLINE")
m.broadcast(404, "pt", termId, "fill", 1, 3, 30, 1, "-")
m.broadcast(404, "pt", termId, "print", "Warning: Reactor Hot!")
m.broadcast(404, "pt", termId, "color", 0x00FF66, 0x08150A)
m.broadcast(404, "pt", termId, "beep", 1.2)
m.broadcast(404, "pt", termId, "title", "REMOTE PAGER")
```
Pass `"all"` as `termId` to broadcast to all pocket terminals in wireless range!
