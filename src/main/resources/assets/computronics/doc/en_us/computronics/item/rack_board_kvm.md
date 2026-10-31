# Server Rack KVM Switch Module

An intelligent 4-port Keyboard-Video-Mouse (KVM) switch module for the OpenComputers **Server Rack**.

Allows centralizing control of up to 4 server blades in the rack. Players can interactively cycle through connected servers by right-clicking the module face on the server rack, or control routing programmatically via OpenOS scripts.

## Component Name
`rack_kvm`

## Methods
* `getSelectedServer(): number` - Returns index of the currently active routed server blade (0-3).
* `setSelectedServer(slot: number): boolean` - Routes console focus to the specified server blade (0-3).
* `getChannelCount(): number` - Returns total channel capacity (4).

## Signals
When the active channel is changed (either via click on the rack face or programmatically):
* `rack_kvm_switch(slot: number)`

## Example
```lua
local component = require("component")
local event = require("event")
local kvm = component.rack_kvm

-- Switch to server blade 2
kvm.setSelectedServer(2)
print("KVM active server: " .. kvm.getSelectedServer())
```
