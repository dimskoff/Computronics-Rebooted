# Server Rack High-Density Capacitor Board

A server rack mountable high-density energy storage module.

Installs into any of the 4 slots of an OpenComputers **Server Rack**. Provides an internal uninterruptible power supply (UPS) buffer holding up to 50,000 EU to keep rack servers and peripheral boards running through brownouts and grid failures.

## Component Name
`capacitor_board`

## Methods
* `getEnergy(): number` - Returns current stored energy buffer in EU.
* `getMaxEnergy(): number` - Returns maximum capacitor storage capacity (50,000 EU).

## Example
```lua
local component = require("component")
local cap = component.capacitor_board

local energy = cap.getEnergy()
local maxEnergy = cap.getMaxEnergy()
print(string.format("Rack Power Buffer: %.0f / %.0f EU (%.1f%%)", energy, maxEnergy, (energy / maxEnergy) * 100))
```
