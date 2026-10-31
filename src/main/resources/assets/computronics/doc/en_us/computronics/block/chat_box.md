# Chat Box

The **Chat Box** connects your computer system to the Minecraft chat. It can broadcast announcements to all nearby players, whisper private messages to specific players, and detect incoming messages typed by players in chat.

Component name: `chat_box`

## Methods

- `say(message: string[, distance: number]): boolean`  
  Sends a message to global chat (or limited to `distance` blocks if provided).
- `tell(player: string, message: string): boolean`  
  Sends a private whisper directly to the named player. Returns `true` if player was found online.
- `getDistance(): number`  
  Gets current maximum listening/broadcasting radius (default: 64 blocks).
- `setDistance(distance: number): number`  
  Sets maximum broadcast distance in blocks. Pass `0` for server-wide unlimited chat.
- `getName(): string`  
  Gets the Chat Box's custom display prefix.
- `setName(name: string): string`  
  Sets custom chat prefix (e.g. `"[Security System]"`).

## Signals

- `chat_message(player: string, message: string, distance: number)`  
  Fired whenever a player sends a message in chat within the Chat Box's detection range.

## Chatbot Lua Example

```lua
local event = require("event")
local component = require("component")
local chat = component.chat_box

chat.setName("[AI Assistant]")
chat.say("Assistant online. Type '!ping' or '!time'.")

while true do
  local _, player, message, dist = event.pull("chat_message")
  print(string.format("[%s] (dist: %.1f): %s", player, dist, message))

  if message == "!ping" then
    chat.tell(player, "Pong! System active.")
  elseif message == "!time" then
    chat.say("Local server time: " .. os.date())
  end
end
```
