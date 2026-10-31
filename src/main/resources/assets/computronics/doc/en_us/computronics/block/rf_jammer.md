# RF Jammer

The **RF Jammer** is a radio frequency interference transmitter. When enabled, it floods the electromagnetic spectrum with noise, corrupting wireless packets and modem signals transmitted by nearby computers, drones, and tablets.

Component name: `rf_jammer`

## Jamming Mechanics
- **Targeted Frequency**: The jammer can be tuned to a specific port/frequency to suppress enemy telecommunications, or set to `0` for broad-spectrum jamming.
- **Noise Corruption**: When packets pass through a jammed zone, strings and byte streams are corrupted with electromagnetic interference (e.g. `"Hello World"` becomes `"H#ll% W0r&d"` or is dropped).
- **Power & Range**: Effective radius is calculated as $\min(64, \sqrt{\text{power}} \times 6)$. Operating at high power consumes continuous energy from the computer network buffer.

## Methods

- `getFrequency(): number` / `setFrequency(freq: number): number`  
  Gets or sets the targeted frequency. Set to `0` to jam all frequencies across the entire spectrum.
- `getPower(): number` / `setPower(power: number): number`  
  Gets or sets transmission power (range: `1.0` to `100.0`).
- `isEnabled(): boolean` / `setEnabled(state: boolean): boolean`  
  Enables or disables radio transmission.
- `getEffectiveRadius(): number`  
  Calculates current interference radius in blocks based on power.

## Lua Example

```lua
local component = require("component")
local jammer = component.rf_jammer

-- Configure tactical broad-spectrum jamming
jammer.setFrequency(0)
jammer.setPower(64.0)
jammer.setEnabled(true)

print(string.format("Jammer ACTIVE! Jamming radius: %.1f blocks.", jammer.getEffectiveRadius()))
```
