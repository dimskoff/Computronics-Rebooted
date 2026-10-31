# Miniature Indicator Lamp

A compact surface-mountable RGB LED indicator lamp.

Can be mounted on existing **Modular Control Panels** or placed directly against any solid wall or block surface. When placed on a wall, it automatically constructs a modular control panel backing node that seamlessly links into the OpenComputers cable bus network.

## Placement & Usage
* **Right-click** on a block face: Mounts a miniature indicator lamp. If targeting an existing Modular Control Panel, mounts onto that specific 8x8 grid slot. If targeting any solid surface, creates a new control panel with the lamp pre-installed.
* **Sneak + Right-click** with an empty hand or tool: Dismantles the lamp and drops it as an item.

## Programming
When connected via a Modular Control Panel to an OpenComputers computer:
```lua
local component = require("component")
local cp = component.control_panel

-- Set slot 0 to pure bright green (24-bit RGB: 0x00FF00)
cp.setLampColor(0, 0x00FF00)

-- Turn off slot 0
cp.setLampColor(0, 0x000000)
```
Each lamp casts true dynamic light when illuminated and renders raised 3D emissive LED optics in the world.
