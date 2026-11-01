# Computronics: Rebooted

<p align="center">
  <img src="src/main/resources/assets/computronics/textures/block/tape_drive_front.png" width="96" height="96" alt="Computronics Logo" />
</p>

<p align="center">
  <a href="https://github.com/DimskOfficial/Computronics-Rebooted/actions"><img src="https://img.shields.io/github/actions/workflow/status/DimskOfficial/Computronics-Rebooted/build-and-release.yml?branch=1.21.1&style=flat-square&label=Build%20Status" alt="Build Status"></a>
  <a href="https://neoforged.net/"><img src="https://img.shields.io/badge/NeoForge-1.21.1-blue?style=flat-square" alt="NeoForge 1.21.1"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="MIT License"></a>
  <a href="docs/WIKI.md"><img src="https://img.shields.io/badge/Wiki-Documentation-orange?style=flat-square" alt="Documentation"></a>
</p>

**Computronics: Rebooted** is the next-generation, high-performance port of the legendary **Computronics** mod for **Minecraft 1.21.1** powered by **NeoForge** and fully integrated with **OpenComputers (1.21.1 Modern Port)**.

Bringing industrial hardware, analog and digital audio streaming, physical tactile interfaces, server rack infrastructure, cryptography, and wireless telemetry into the modern era of Minecraft modding.

---

## 📚 Complete Documentation
For exhaustive hardware specifications, OpenComputers API callbacks, signals, and Lua examples, see our [**Official Wiki & Technical Reference**](docs/WIKI.md).

---

## 🚀 Key Features & Hardware Modules

### 🎛️ Modular Control Panel System
* **Ultra-thin Surface Mounting:** Mounts flat against floors, ceilings, and walls (2 voxels thick) with zero collision clipping or wall penetration.
* **8×8 Tactile Grid (64 Slots):** Mount miniature tactile push buttons, RGB indicator lamps, toggle switches, 7-segment displays, and text labels anywhere on the grid.
* **Tactile Mechanical Feedback:** Buttons physically depress into the panel when clicked, accompanied by realistic mechanical click audio (`button_press` / `button_release`).
* **RGB Status Lamps:** Software-controlled multi-color LEDs with full-bright emissive rendering and customizable 3D text labels.
* **Slanted 7-Segment Displays:** MicroTechnics-accurate proportions with trapezoid segments, italic slant (~6.3°), unlit segment acrylic traces, and multi-digit formats (`1x1`, `1x2`, `1x3`, `1x4`).
* **Mountable Text Component (`ItemPanelText`):** Pure 3D text labels without bulky borders in sizes `1x1`, `2x1`, `4x1`, `8x1`, `2x2`, `4x2`.
* **Screwdriver Tool (`ItemScrewdriver`):** 
  - **Left-Click:** Dismantles individual components under crosshair safely without breaking the block.
  - **Right-Click:** Opens modern, dark-industrial 350×240 GUI with real-time preview and color swatches.
  - **HUD Overlay:** Displays component slot ID, coordinates `(X, Y)`, and active status.
* **Event System:** Dispatches `panel_button(slot, player, x, y)` and `panel_switch(slot, state, player, x, y)` signals directly over the OpenComputers network bus.

### 📼 Tape Drive & Digital Audio Streaming
* **Digital DFN Audio:** Streams 8-bit digital audio cassettes directly into Minecraft's sound engine.
* **Animated Cassette Slot:** Inserting a cassette smoothly slides it deep into the mechanical chassis.
* **Tape Operations:** Full support for `play`, `stop`, `pause`, `seek`, `rewind`, and raw 8-bit read/write streaming.
* **Cassette Tapes:** Tiered from Iron (4 minutes), Gold (8 minutes), Diamond (32 minutes) up to Nether Star (128 minutes).

### 📟 Wireless VRAM Pager / Pocket Terminal
* **Ergonomic One-Handed First-Person Rendering:** Held naturally in the player's right hand like a map in a boat, with natural tilt and rock-solid view during movement.
* **Dual-Layer Matrix CRT:** High-contrast retro green phosphor matrix display (32 columns × 12 rows text).
* **Wireless Network Receiver:** Listens for telemetry packets sent over OpenComputers wireless network cards.
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

### 🗣️ Clean Text-to-Speech Engine
* **Speech Box Block & Robot Upgrade:** Synthesizes clear narrator voice speech in Russian and English with zero annoying legacy phoneme beeps.

### 📷 Peripherals, Sensors & Defense Modules
* **Camera:** 3D raycasting with distance measurements and block reflectance analysis.
* **Chat Box:** Secure in-game chat reading and broadcasting.
* **Radar & Entity Detector:** Detect nearby entities, players, and coordinates.
* **RF Jammer Card & Block:** Radio frequency interference module to jam wireless network packets within range.
* **Laser Transceiver:** Optical line-of-sight high-bandwidth network bridges.
* **Weather Station:** Meteorological sensors reporting weather, temperature, and rain levels.
* **Analog Gauge & Iron Note Block:** Vintage physical indicators and metallic chiptune synthesizers.

---

## 💻 OpenOS Lua Programming Quickstart

### 1. Control Panel Automation
```lua
local component = require("component")
local event = require("event")
local panel = component.control_panel

-- Set a 7-segment display to "10 10" in cyan
panel.setColor(1, 0x00FFCC)
panel.setSegment(1, "10 10")

-- Set a green indicator lamp with a "RUN" text label
panel.setColor(5, 0x00FF40)
panel.setText(5, "RUN")
panel.setLamp(5, true)

-- Listen for tactile button clicks
while true do
  local _, _, slot, player, gx, gy = event.pull("panel_button")
  print(string.format("Button pressed at (%d, %d) by %s", gx, gy, player))
end
```

### 2. Digital Cassette Player
```lua
local component = require("component")
local tape = component.tape_drive

if tape.isReady() then
  tape.rewind()
  tape.setVolume(1.0)
  tape.play()
  print("Playing cassette: " .. tape.getLabel())
end
```

---

## 🛠️ Building & Compilation

### Requirements
* **Java 21 or Java 25 (JDK)**
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
Licensed under the [MIT License](LICENSE).  
Original Computronics by **asiekierka** and **Vexatos**. Ported and modernized for Minecraft 1.21.1 NeoForge by **Dimsk** and contributors.
