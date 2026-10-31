# Packet Sniffer Card

The **Packet Sniffer Card** is a network diagnostics and cyber-surveillance expansion card. It operates like a hardware TAP (Test Access Point), capturing and analyzing all network traffic traversing the component network without requiring open ports.

Component name: `packet_sniffer`

## Modes & Features
- **Promiscuous TAP Mode**: When promiscuous mode is enabled (`true`), every single unicast and broadcast packet on the network cable is captured and signaled to the computer.
- **Traffic Telemetry**: Tracks real-time bandwidth in Bytes Per Second (BPS) and frame throughput in Packets Per Second (PPS).
- **Target Filtering**: Can be configured to capture only packets matching a specific port or sender address prefix.

## Methods

- `getBandwidth(): number`  
  Returns current bandwidth in Bytes Per Second (BPS).
- `getPacketsPerSecond(): number`  
  Returns current throughput in Packets Per Second (PPS).
- `getTotalBytes(): number`  
  Returns cumulative bytes recorded since insertion/reset.
- `getTotalPackets(): number`  
  Returns cumulative packet count.
- `isPromiscuous(): boolean` / `setPromiscuous(enabled: boolean): boolean`  
  Toggles promiscuous full-mirror capture mode.
- `setFilterPort(port: number): number`  
  Sets port filter (pass `0` for all ports).
- `setFilterSender(sender: string): string`  
  Filters packets to those starting with the given sender address.
- `resetStats(): boolean`  
  Clears traffic counters.

## Signals

- `packet_sniffed(source: string, destination: string, port: number, size: number, summary: string)`  
  Emitted for every captured packet. `destination` is `"broadcast"` for broadcasts, or the target computer address. `summary` provides a preview of the packet payload.

## Lua Example

```lua
local event = require("event")
local component = require("component")
local sniffer = component.packet_sniffer

sniffer.setPromiscuous(true)
print("TAP packet capture active. Listening for wire traffic...")

while true do
  local _, src, dst, port, size, data = event.pull("packet_sniffed")
  print(string.format("[%s -> %s] Port: %d (%d B): %s", src:sub(1,8), dst:sub(1,8), port, size, data))
  print(string.format("  Current Bandwidth: %d BPS | Packet Rate: %d PPS",
    sniffer.getBandwidth(), sniffer.getPacketsPerSecond()))
end
```
