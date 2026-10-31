# Server Rack Light Board

A server rack mountable board featuring 4 programmable multi-color RGB status LEDs.

Installs into any of the 4 slots of an OpenComputers **Server Rack**.

## Component Name
`light_board`

## Methods
* `setLightColor(index: number, color: number): boolean` - Sets 24-bit RGB color for LED index (0-3).
* `getLightColor(index: number): number` - Gets RGB color of LED index (0-3).
* `setColors(colors: table): boolean` - Sets colors of all lights simultaneously from a 1-indexed table.
* `getLightCount(): number` - Returns total number of LEDs (4).
* `setMode(mode: number): number` - Sets indicator display animation mode (1-5).
* `getMode(): number` - Gets current display animation mode.
* `clear(): boolean` - Turns off all LEDs.

## Example
```lua
local component = require("component")
local lights = component.light_board

-- Set first light to green, second to red
lights.setLightColor(0, 0x00FF00)
lights.setLightColor(1, 0xFF0000)
```
