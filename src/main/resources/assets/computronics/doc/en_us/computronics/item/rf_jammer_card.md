# RF Signal Jammer Card

The **RF Signal Jammer Card** is an expansion card for computers, tablets, drones, and mobile robots that emits high-power electromagnetic interference to scramble and corrupt wireless communications within an adjustable radius.

Component name: `rf_jammer`

## Methods

- `setEnabled(enabled: boolean): boolean`  
  Activates or deactivates active signal jamming transmission.
- `isEnabled(): boolean`  
  Returns whether the jammer is currently broadcasting.
- `setFrequency(freq: number): number`  
  Sets targeted transmission frequency (`0` for broad-spectrum jamming).
- `getFrequency(): number`  
  Returns the currently jammed radio frequency.
- `setPower(power: number): number`  
  Sets transmission power (`1.0` to `100.0`).
- `getPower(): number`  
  Returns current transmission power.
- `getRadius(): number`  
  Returns the effective interference radius in blocks (`power * 0.75`).
- `isAreaJammed([freq: number]): boolean`  
  Checks if the current location is subject to active radio frequency interference.

## Lua Example

```lua
local component = require("component")
local jammer = component.rf_jammer

-- Set to disrupt all frequencies in a 30m radius
jammer.setFrequency(0)
jammer.setPower(40)
jammer.setEnabled(true)
print("Mobile RF Jamming active! Effective radius: " .. jammer.getRadius() .. "m")
```
