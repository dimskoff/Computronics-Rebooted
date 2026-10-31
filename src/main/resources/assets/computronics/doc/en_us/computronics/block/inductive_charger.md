# Inductive Charger

The **Inductive Charger** is a wireless charging platform. It transfers power directly into drones and robots that are flying, hovering, or resting within a 2-block column above the charging pad. Multiple chargers can be placed side-by-side to construct large helipads.

Component name: `inductive_charger`

## Methods

- `isCharging(): boolean`  
  Checks if wireless power transmission is enabled.
- `setCharging(state: boolean): boolean`  
  Enables or disables wireless charging.
- `getChargeRate(): number`  
  Gets maximum energy transfer rate per tick (default: 100 EU/t).
- `setChargeRate(rate: number): number`  
  Adjusts energy transfer rate (range: `1.0` to `1000.0` EU/t).

## Lua Example

```lua
local component = require("component")
local charger = component.inductive_charger

charger.setChargeRate(250.0)
charger.setCharging(true)
print("Inductive Helipad charging enabled at 250 EU/tick.")
```
