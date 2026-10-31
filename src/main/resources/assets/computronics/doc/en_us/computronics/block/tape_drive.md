# Tape Drive

The **Tape Drive** is an industrial audio and binary storage device capable of interfacing with magnetic audio tapes. It connects to the OpenComputers network via cables or by placing it adjacent to a computer.

Component name: `tape_drive`

## Operation
- **Insert Tape**: Right-click the Tape Drive with a magnetic tape in hand.
- **Eject Tape**: Right-click the Tape Drive with an empty hand, or invoke `eject()` via Lua code.

## Component Methods

- `isReady(): boolean`  
  Returns `true` if a magnetic tape is currently inserted into the drive.
- `getSize(): number`  
  Returns the total capacity of the inserted tape in bytes (e.g. 131072 for Iron, 1048576 for Nether Star).
- `getPosition(): number`  
  Returns the current byte position of the read/write head.
- `seek(offset: number): number`  
  Moves the read/write head by the specified amount of bytes relative to current position. Returns actual bytes moved.
- `read([count: number]): string`  
  Reads up to `count` bytes (default: 1024) from current position.
- `write(data: string): number`  
  Writes the given string or binary bytes onto the tape. Returns the number of bytes successfully written.
- `getState(): string`  
  Returns current state: `"STOPPED"` or `"PLAYING"`.
- `play(): boolean`  
  Begins playing the tape audio track from the current head position.
- `stop(): boolean`  
  Halts playback and audio streaming.
- `rewind(): boolean`  
  Rewinds the tape to the start (position 0).
- `getSpeed(): number` / `setSpeed(speed: number): number`  
  Gets or sets playback speed (range: `0.25` to `2.0`, normal is `1.0`).
- `getVolume(): number` / `setVolume(volume: number): number`  
  Gets or sets audio volume (range: `0.0` to `1.0`).
- `getLabel(): string` / `setLabel(label: string): string`  
  Gets or sets the tape's custom title.
- `eject(): boolean`  
  Ejects the inserted tape into the world as an item.

## OpenOS Program

A built-in `tape` utility program is available on the Computronics loot disk:
```sh
tape play              # Start playback
tape pause             # Pause playback
tape stop              # Stop playback and rewind
tape rewind            # Rewind tape head to 0
tape speed 1.5         # Set playback speed
tape volume 0.8        # Set playback volume
tape label "Backup 1"  # Label the tape
tape write music.dfpwm # Write file to tape
tape wipe              # Erase tape contents
```

## Lua Example

```lua
local component = require("component")
local tape = component.tape_drive

if not tape.isReady() then
  error("Please insert a tape first!")
end

print("Tape Size: " .. tape.getSize() .. " bytes")
tape.rewind()
tape.write("Hello Computronics!")
tape.rewind()
print("Read back: " .. tape.read(18))
```
