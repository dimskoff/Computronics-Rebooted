# Radar

The **Radar** block is an active sensor that scans its surroundings in a 3D spherical volume. It can detect both players and living entities, returning their relative coordinates and distances.

Component name: `radar`

## Energy Requirements
Active scanning consumes energy from the computer network proportional to the requested radius (~1.5 to 3.0 EU per block scanned). Ensure sufficient network power. Maximum scan radius is 64 blocks.

## Methods

- `getEntities([radius: number]): table`  
  Scans for all living entities (mobs, animals, players) within `radius` (default: 16, max: 64). Returns an array of tables:
  ```lua
  {
    name = "Zombie",
    type = "minecraft:zombie",
    x = 4.2,
    y = -1.0,
    z = 8.5,
    distance = 9.5
  }
  ```
- `getPlayers([radius: number]): table`  
  Specialized query scanning specifically for online players within `radius` (default: 16, max: 64). Returns an array of tables with player names, relative coordinates, and distances.

## Lua Example

```lua
local component = require("component")
local radar = component.radar

local targets = radar.getPlayers(32)
if not targets then
  print("Radar error: not enough energy.")
  return
end

print("Detected players within 32 blocks: " .. #targets)
for _, p in ipairs(targets) do
  print(string.format("Player %s at (%.1f, %.1f, %.1f), dist: %.1fm", p.name, p.x, p.y, p.z, p.distance))
end
```
