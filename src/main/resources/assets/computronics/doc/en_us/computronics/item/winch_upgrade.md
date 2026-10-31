# Drone Winch Upgrade

The **Drone Winch Upgrade** equips OpenComputers drones with a motorized steel cable and mechanical grabber hook. It enables drones to hover high above terrain while lowering a cable down into caverns or over cargo containers to grab items and entities.

Component name: `winch`

## Cable Mechanics
- **Maximum Length**: Can deploy up to **32 blocks** downward.
- **Grab Hook**: Grabs items and living entities within a 1-block radius beneath the lowered hook.

## Methods

- `deploy(blocks: number): number`  
  Extends the winch cable downward by the specified block count (max: 32). Returns new deployed length.
- `retract(blocks: number): number`  
  Winds the winch cable back up toward the drone. Returns new deployed length.
- `getDeployedLength(): number`  
  Returns current cable extension in blocks (`0` is fully retracted).
- `grab(): boolean`  
  Attempts to close the hook around an entity or item at the hook's position. Returns `true` if an entity was secured.
- `release(): boolean`  
  Opens the hook and releases the attached cargo.

## Lua Example

```lua
local component = require("component")
local winch = component.winch

-- Deploy cable 10 blocks down
winch.deploy(10)
print("Cable deployed: " .. winch.getDeployedLength() .. "m. Grabbing target...")

if winch.grab() then
  print("Cargo secured! Retracting cable...")
  winch.retract(10)
  print("Cargo hoisted to drone.")
else
  print("Nothing detected beneath the hook.")
  winch.retract(10)
end
```
