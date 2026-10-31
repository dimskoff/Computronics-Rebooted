# Tier 3 Network Card

The **Tier 3 Network Card** is an enterprise-grade Ethernet controller for OpenComputers cases, servers, and racks. It substantially upgrades network throughput and grants low-level frame transmission controls.

Component name: `modem`

## Specifications
- **Jumbo Frames**: Supports packet sizes up to **65536 bytes (64 KB)**, allowing large data tables and programs to transfer in a single frame.
- **Port Capacity**: Allows up to **64 ports** to be kept open concurrently.
- **Source Address Spoofing**: Hardware support for forging the sender address on outgoing unicast and broadcast frames.

## Methods

- `open(port: number): boolean`  
  Opens the specified network port (up to 64 active ports).
- `close([port: number]): boolean`  
  Closes the specified port (or all ports if omitted).
- `isOpen(port: number): boolean`  
  Checks if the port is open.
- `maxPacketSize(): number`  
  Returns `65536` (64 KB).
- `maxOpenPorts(): number`  
  Returns `64`.
- `isSpoofingSupported(): boolean`  
  Returns `true`.
- `send(target: string, [source: string], port: number, data...): boolean`  
  Transmits data directly to `target`. If `source` is provided, the packet is transmitted with the spoofed sender address.
- `broadcast([source: string], port: number, data...): boolean`  
  Broadcasts data across the entire network with optional spoofed source address.
- `getWakeMessage(): string, boolean`  
  Gets the computer wake-up message and fuzzy matching flag.
- `setWakeMessage(msg: string[, fuzzy: boolean]): string, boolean`  
  Sets wake-up message.

## Signals

- `modem_message(localAddress: string, remoteAddress: string, port: number, distance: number, data...)`  
  Fired when a packet arrives on an open port.

## Lua Example

```lua
local component = require("component")
local modem = component.modem

modem.open(80)

-- Send a packet appearing to come from an administrator address:
modem.send("target-computer-address", "fake-admin-address", 80, "AUTHORIZED_PAYLOAD")

-- Broadcast spoofed ping:
modem.broadcast("gateway-router", 80, "ROUTER_HEARTBEAT")
```
