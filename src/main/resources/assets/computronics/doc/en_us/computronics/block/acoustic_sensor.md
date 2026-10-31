# Acoustic Sensor

The **Acoustic Sensor** is an acoustic detection block built upon Minecraft 1.21.1's native GameEvent vibration architecture (similar to Sculk Sensors). It detects footstep vibrations, projectile impacts, door openings, and block breaks in real time.

Component name: `acoustic_sensor`

## Vibration Mechanics
- Detects vibrations according to the Minecraft GameEvent frequency spectrum.
- **Sneaking / Wool**: Walking while sneaking (crouching) or walking across wool carpets suppresses vibration emissions, bypassing detection just like standard sculk mechanics.

## Methods

- `getSensitivity(): number`  
  Returns current detection radius (default: 16 blocks, max: 32).
- `setSensitivity(range: number): number`  
  Adjusts detection radius (range: `1` to `32`).
- `getLastVibration(): table`  
  Returns full information regarding the most recent detected event:
  ```lua
  {
    event = "minecraft:step",
    distance = 4.2,
    frequency = 1,
    x = 100, y = 64, z = 200,
    entity = "PlayerName"
  }
  ```

## Signals

- `vibration(event: string, distance: number, frequency: number, x: number, y: number, z: number)`  
  Fired whenever an acoustic vibration reaches the sensor.

## Lua Example

```lua
local event = require("event")
local component = require("component")
local sensor = component.acoustic_sensor

sensor.setSensitivity(24)
print("Acoustic perimeter active (radius: 24m). Listening for vibrations...")

while true do
  local _, evName, dist, freq, x, y, z = event.pull("vibration")
  print(string.format("Sound Detected! Event: %s (Freq: %d) at (x:%d, y:%d, z:%d), Dist: %.1fm",
    evName, freq, x, y, z, dist))
end
```
