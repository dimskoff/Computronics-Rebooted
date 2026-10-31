# Self-Destruct Card

The **Self-Destruct Card** is a thermal demolition expansion card installed into computer cases, servers, or robots. It can trigger an immediate or countdown-timed explosion to prevent unauthorized physical access to proprietary hardware.

Component name: `self_destruct`

## Methods

- `start(fuse: number): boolean`  
  Arms the self-destruct fuse for `fuse` seconds.
- `time(): number`  
  Returns the remaining fuse time in seconds. Returns `-1.0` if unarmed.
- `detonate([power: number]): boolean`  
  Immediately detonates the computer case (default explosion power: 3.0, max: 8.0).
- `setTime(ticks: number[, power: number]): boolean`  
  Configures the countdown in Minecraft game ticks (20 ticks = 1 second) and optional explosion power.

## OpenOS Program

A built-in `explode` program is included on the Computronics loot disk:
```sh
explode 10       # Arms a 10-second fuse with full-screen countdown
explode -s 5     # Arms a silent 5-second fuse without screen display
explode -t 15    # Displays countdown inline in shell without clearing screen
```

## Lua Example

```lua
local component = require("component")
local sd = component.self_destruct

print("ARMING FACILITY SELF-DESTRUCT...")
sd.start(5) -- 5 seconds fuse

while sd.time() > 0 do
  print(string.format("T-minus: %.1f seconds...", sd.time()))
  os.sleep(1)
end
```
