# Miniature Tactile Push Button

A miniature physical tactile push button.

Can be mounted on existing **Modular Control Panels** or placed directly against any solid wall or block surface. When placed on a wall, it automatically constructs a modular control panel backing node that seamlessly links into the OpenComputers cable bus network.

## Placement & Usage
* **Right-click** on a block face: Mounts a miniature button. If targeting an existing Modular Control Panel, mounts onto that specific 8x8 grid slot. If targeting any solid surface, creates a new control panel with the button pre-installed.
* **Right-click** on an installed button with an empty hand: Presses the tactile button, plays a mechanical click sound, and triggers a network event `panel_button` on the computer bus.
* **Sneak + Right-click**: Dismantles the button and drops it as an item.

## Event Handling
When any button on the panel is pressed by a player:
```lua
local event = require("event")

while true do
  local id, address, slot, player = event.pull("panel_button")
  print(string.format("Button at slot %d was pressed by %s!", slot, player))
end
```
Tactile buttons render with distinct 3D mechanical caps on the panel surface.
