# Server Rack Switch Board

A server rack mountable board featuring 4 physical toggle switches.

Installs into any of the 4 slots of an OpenComputers **Server Rack**. Players can flip switches directly by right-clicking on the front panel of the rack, or computers can read and toggle them programmatically.

## Component Name
`switch_board`

## Methods
* `getSwitch(index: number): boolean` - Returns current state of switch (0-3).
* `setSwitch(index: number, state: boolean): boolean` - Sets switch state programmatically and emits a `rack_switch` signal.
* `getSwitchCount(): number` - Returns total number of switches (4).

## Signals
When a player toggles a switch directly on the rack face, or when `setSwitch` is called:
* `rack_switch(index: number, state: boolean)`

```lua
local event = require("event")
while true do
  local _, idx, state = event.pull("rack_switch")
  print(string.format("Switch %d flipped to %s", idx, tostring(state)))
end
```
