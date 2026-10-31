# Laser Transceiver

The **Laser Transceiver** establishes high-speed optical network links over distances of up to 64 blocks without physical cables. Additionally, it doubles as an optical tripwire security sensor.

Component name: `laser_transceiver`

## Alignment & Range
Two Laser Transceivers must face each other in a direct line of sight with no solid blocks between them. When aligned, the transceivers bridge their respective computer networks, transparently forwarding network messages.

## Optical Tripwire
If a player, mob, or entity steps into the optical laser beam:
1. The laser connection is momentarily broken.
2. An alarm signal `tripwire_triggered` is immediately dispatched to connected computers.

## Methods

- `isConnected(): boolean`  
  Returns `true` if an aligned transceiver is linked and the beam is uninterrupted.
- `getDistance(): number`  
  Returns distance in blocks to the linked transceiver, or `-1` if unlinked.
- `isBeamBroken(): boolean`  
  Returns `true` if an entity is currently blocking the optical laser beam.

## Signals

- `tripwire_triggered(entity: string, distance: number)`  
  Fired the moment an entity intercepts the laser beam, reporting entity name and distance along the beam.

## Lua Example

```lua
local event = require("event")
local component = require("component")
local laser = component.laser_transceiver

print("Laser link status: " .. tostring(laser.isConnected()) .. ", Distance: " .. laser.getDistance() .. "m")

while true do
  local _, intruder, dist = event.pull("tripwire_triggered")
  print(string.format("SECURITY BREACH! Optical tripwire tripped by %s at %.1fm!", intruder, dist))
end
```
