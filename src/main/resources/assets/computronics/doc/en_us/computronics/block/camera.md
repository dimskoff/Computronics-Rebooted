# Camera

The **Camera** is an optical sensor capable of raycasting into the world at specified angles (`yaw` and `pitch`). It can measure distance to surface obstacles and analyze target block properties.

Component name: `camera`

## Angles
- `yaw`: Horizontal angle in degrees (`0` to `360`).
- `pitch`: Vertical angle in degrees (`-90` straight down, `0` horizon, `90` straight up).

## Methods

- `distance(yaw: number, pitch: number): number`  
  Fires an optical laser ray in the specified direction up to 64 blocks. Returns exact distance to the first obstacle, or `-1.0` if no solid block was encountered.
- `scan(yaw: number, pitch: number): table`  
  Analyzes the block struck by the raycast. Returns a table:
  ```lua
  {
    distance = 14.2,
    block = "minecraft:obsidian",
    hardness = 50.0,
    light = 15
  }
  ```

## Lua Example

```lua
local component = require("component")
local cam = component.camera

local result = cam.scan(0, 0)
if result then
  print(string.format("Looking forward: hit %s at distance %.2fm (Hardness: %.1f, Light: %d)",
    result.block, result.distance, result.hardness, result.light))
else
  print("Line of sight clear (no block within 64m).")
end
```
