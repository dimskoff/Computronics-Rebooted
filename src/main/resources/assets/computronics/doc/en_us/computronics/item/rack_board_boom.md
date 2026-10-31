# Server Rack Detonator Board

A server rack mountable thermal explosive board designed for remote emergency facility purge or server self-destruction.

Installs into any of the 4 slots of an OpenComputers **Server Rack**.

## Component Name
`boom_board`

## Methods
* `detonate(): boolean` - Immediately detonates the rack board with high blast force.
* `setCountdown(seconds: number): boolean, number` - Arms an emergency countdown in seconds (1 to 60).
* `disarm(): boolean` - Disarms an active countdown.
* `getCountdown(): number` - Returns remaining seconds until blast, or -1 if disarmed.

## Example
```lua
local component = require("component")
local boom = component.boom_board

print("Arming self-destruction in 10 seconds...")
boom.setCountdown(10)
```
