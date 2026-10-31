# Computronics: Rebooted

<p align="center">
  <img src="src/main/resources/assets/computronics/textures/block/tape_drive_front.png" width="96" height="96" alt="Computronics Logo" />
</p>

**Computronics: Rebooted** is the next-generation, high-performance port of the legendary **Computronics** mod for **Minecraft 1.21.1** powered by **NeoForge** and fully integrated with **OpenComputers (1.21.1 Port)**.

Bringing industrial hardware, analog and digital audio streaming, physical tactile interfaces, server rack infrastructure, cryptography, and wireless telemetry into the modern era of Minecraft modding.

---

## 🚀 Key Features & Hardware Modules

### 📼 Tape Drive & Audio Streaming
* **Digital DFN Audio:** Streams 8-bit digital audio cassettes directly from tape storage into the Minecraft sound engine.
* **Animated Cassette Slot:** Inserting a cassette smoothly slides it into the mechanism with realistic mechanical feedback.
* **Tape Operations:** Full support for `play`, `stop`, `pause`, `seek`, `rewind`, and raw 8-bit read/write streaming.
* **Cassette Tapes:** Tiered from Iron (4 minutes) up to Platinum/Netherite (128 minutes).

### 🎛️ Modular Control Panel (Flat Surface Plate)
* **Ultra-thin Surface Mounting:** Mounts flat against floors, ceilings, and walls (2 voxels thick) with zero collision clipping.
* **8×8 Tactile Grid (64 Slots):** Mount miniature tactile push buttons and RGB indicator lamps anywhere on the grid.
* **Tactile Mechanical Feedback:** Buttons visibly depress into the panel when clicked, accompanied by realistic mechanical click audio (`button_press` / `button_release`).
* **RGB Status Lamps:** Software-controlled multi-color LEDs with full-bright emissive rendering.
* **Event System:** Dispatches `panel_button(slot, player, x, y)` signals directly over the OpenComputers network bus.

### 📟 Wireless VRAM Pager / Pocket Terminal
* **Ergonomic One-Handed First-Person Rendering:** Held naturally in the player's right hand like a map in a boat, with natural ergonomic tilt and no re-equip animation jitter.
* **Dual-Layer Matrix CRT:** High-contrast retro green phosphor matrix display (32 columns × 12 rows text).
* **Wireless Network Receiver:** Listens for packets sent over OpenComputers wireless network cards.
* **Inductive Charging:** Recharges wirelessly when placed above an Inductive Charger Pad.

### ⚡ Inductive Charger Pad
* **Wireless Energy Induction:** Wirelessly charges handheld terminals and battery-operated robotic devices in a 3-block radius.

### 🖥️ Server Rack Infrastructure
Computronics server rack modules plug directly into OpenComputers Server Racks:
* **Light Board:** Customizable status LEDs on the rack chassis.
* **Switch Board:** Physical toggle switches to direct network flows.
* **Rack Capacitor:** High-density rack energy buffer.
* **Rack Cryptographic Co-Processor:** Hardware-accelerated MD5, SHA-256, CRC32, AES-128 cryptographic operations.
* **Rack KVM Switch Module:** Interactive 4-channel KVM switch to toggle peripherals and keyboard/video streams between mounted servers.

### 📷 Camera & Raycasting
* **Distance Scanning:** 3D raycasting with distance measurements and block reflectance analysis.

### 💬 Chat Box & Speech Box
* **Chat Box:** Secure in-game chat reading and broadcasting.
* **Speech Box:** Text-to-speech audio synthesis directly into the player's vicinity.

### 📡 Wireless & Defense
* **Radar & Entity Detector:** Detect nearby entities, players, and coordinates.
* **RF Jammer Card / Block:** Radio frequency interference module to jam wireless network packets within range.
* **Laser Transceiver:** Optical line-of-sight high-bandwidth network bridges.
* **Weather Station:** Meteorological sensors reporting weather, temperature, and rain levels.

---

## 💻 OpenOS Lua Programming Examples

### 1. Modular Control Panel
```lua
local component = require("component")
local event = require("event")
local panel = component.control_panel

-- Turn on a green status lamp at (2, 3)
panel.setLampColor(2, 3, 0x00FF00)
panel.setLamp(2, 3, true)

-- Listen for button clicks
while true do
  local _, _, slot, player, gx, gy = event.pull("panel_button")
  print(string.format("Player %s pressed button at (%d, %d) [Slot #%d]", player, gx, gy, slot))
  -- Toggle lamp on button press
  panel.setLampColor(gx, gy, 0x00FFFF)
  panel.setLamp(gx, gy, true)
end
```

### 2. Wireless VRAM Pager Telemetry
```lua
local component = require("component")
local modem = component.modem

modem.open(404)
-- Send text line to the Pocket Terminal
modem.broadcast(404, "vram_print", 0, "REACTOR CORE: NORMAL", 0x00FF66)
modem.broadcast(404, "vram_print", 1, "POWER: 98.4% [1240 EU/t]", 0x00FFAA)
```

### 3. Tape Drive Playback
```lua
local component = require("component")
local tape = component.tape_drive

if tape.isReady() then
  tape.rewind()
  tape.play()
  print("Playing tape: " .. tape.getLabel())
end
```

---

## 🛠️ Building & Compilation

### Requirements
* **Java 21 or Java 25**
* **Gradle 8.5+** (Included via `gradlew`)
* **NeoForge 1.21.1** (Configured via NeoGradle)

### Build Command
```bash
./gradlew build -x test
```
The compiled mod JAR will be located in:
`build/libs/computronics-2.0.0-dev.1.jar`

---

## 📜 License
Licensed under the MIT License / AsieLib Open Source License.
Original Computronics by **asiekierka** and **Vexatos**. Ported and modernized for Minecraft 1.21.1 NeoForge.
