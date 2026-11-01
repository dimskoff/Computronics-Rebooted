# Computronics: Rebooted — Complete Official Wiki & Technical Reference

**Minecraft Version:** 1.21.1  
**Mod Loader:** NeoForge  
**Integration:** OpenComputers (1.21.1 Modern Port)  
**Mod ID:** `computronics`  

---

## 📑 Table of Contents
1. [Overview & Architecture](#1-overview--architecture)
2. [Modular Control Panel System](#2-modular-control-panel-system)
   - [Panel Mounting & Surface Physics](#panel-mounting--surface-physics)
   - [Tactile Push Buttons](#tactile-push-buttons)
   - [Indicator RGB Lamps](#indicator-rgb-lamps)
   - [Industrial Toggle Switches](#industrial-toggle-switches)
   - [7-Segment Slanted Digital Displays](#7-segment-slanted-digital-displays)
   - [Panel Text Labels](#panel-text-labels)
   - [Screwdriver & Configuration GUI](#screwdriver--configuration-gui)
   - [OpenComputers `control_panel` API](#opencomputers-control_panel-api)
3. [Tape Drive & Digital Audio Streaming](#3-tape-drive--digital-audio-streaming)
   - [Cassette Tapes & Durations](#cassette-tapes--durations)
   - [DFN 8-Bit Audio Format](#dfn-8-bit-audio-format)
   - [Animated Cassette Insertion](#animated-cassette-insertion)
   - [OpenComputers `tape_drive` API](#opencomputers-tape_drive-api)
4. [Handheld Terminals & Pagers](#4-handheld-terminals--pagers)
   - [Pocket Terminal / VRAM Pager](#pocket-terminal--vram-pager)
   - [One-Handed First-Person Rendering](#one-handed-first-person-rendering)
   - [Inductive Charging](#inductive-charging)
5. [Server Rack Infrastructure](#5-server-rack-infrastructure)
   - [Light Board](#light-board)
   - [Switch Board](#switch-board)
   - [Rack Capacitor](#rack-capacitor)
   - [Rack Cryptographic Co-Processor](#rack-cryptographic-co-processor)
   - [Rack KVM Switch Module](#rack-kvm-switch-module)
6. [Speech Box & Audio Upgrades](#6-speech-box--audio-upgrades)
   - [Real Text-to-Speech Engine](#real-text-to-speech-engine)
   - [Speech Box Block & Robot Upgrade](#speech-box-block--robot-upgrade)
7. [Peripherals, Sensors & Defense Modules](#7-peripherals-sensors--defense-modules)
   - [Chat Box](#chat-box)
   - [Radar & Entity Detector](#radar--entity-detector)
   - [Camera & Raycasting](#camera--raycasting)
   - [RF Jammer Card & Block](#rf-jammer-card--block)
   - [Laser Transceiver](#laser-transceiver)
   - [Weather Station](#weather-station)
   - [Analog Gauge](#analog-gauge)
   - [Iron Note Block](#iron-note-block)
8. [Hardware Upgrades & Cards](#8-hardware-upgrades--cards)
9. [OpenOS Lua Software Suite (`computronics.lua`)](#9-openos-lua-software-suite-computronicslua)

---

## 1. Overview & Architecture

Computronics: Rebooted connects physical Minecraft mechanics with high-performance OpenComputers computing. Every peripheral acts as a first-class node on the OpenComputers network bus, capable of communicating with Computer Cases, Server Racks, Microcontrollers, Robots, and Drones.

All components utilize client-side rendering optimizations, event-driven network packets, and modern NeoForge 1.21.1 protocols.

---

## 2. Modular Control Panel System

### Panel Mounting & Surface Physics
- **Block:** `computronics:control_panel`
- **Mounting:** Can be placed on any block face (**Floor, Ceiling, Walls** in North, South, West, East orientations).
- **Profile:** Ultra-flat 2-voxel high industrial plate (1/8 of a block) that attaches seamlessly without clipping into walls or adjacent blocks.
- **Grid Layout:** An 8×8 grid offering 64 discrete mountable component slots (slots indexed 1 through 64).

### Mountable Components

#### Tactile Push Buttons (`ItemMiniButton`)
- **Sizes:** `1x1`, `2x2`, `3x3`, `4x4` (cycled via `Shift + Right-Click` in hand).
- **Physical Feedback:** Visibly depresses into the panel for 10 ticks (0.5s) upon clicking, accompanied by mechanical click sounds (`button_press` and `button_release`).
- **3D In-World Text:** Configurable text label rendered directly on top of the button cap in any RGB hex color and font scale.
- **Signals:** Dispatches `panel_button(slot, player, x, y)` over the network.

#### Indicator RGB Lamps (`ItemMiniLamp`)
- **Sizes:** `1x1`, `2x2`, `3x3`, `4x4`.
- **Lighting:** Flat sleek indicator lens with glowing center core at full emissive brightness (`LightTexture.FULL_BRIGHT`).
- **Text Labels:** Supports 3D text labels rendered across the lens surface.
- **State & Color:** Software-controllable on/off state and 24-bit RGB color.

#### Industrial Toggle Switches (`ItemToggleSwitch`)
- **Sizes:** `1x1`, `2x2`.
- **Mechanism:** Dual-position latching rocker switch. Tilts forward for ON, tilts back for OFF.
- **Visuals:** Illuminated green status LED dot when toggled ON.
- **Signals:** Dispatches `panel_switch(slot, state, player, x, y)` over the network.

#### 7-Segment Slanted Digital Displays (`ItemSevenSegment`)
- **Slender Proportions:** Engineered to match industry-standard LED segment diagrams (~1:1.8 width-to-height ratio).
- **Trapezoid Segments:** Chamfered segments `A`–`F`, central hexagon `G`, and bottom-right decimal point `DP`.
- **Italic Slant:** Slanted at ~6.3° for authentic vintage instrument aesthetics.
- **Unlit Segments:** De-energized segments remain subtly visible as realistic tinted acrylic traces.
- **Digit Formats:** Available in `1x1`, `1x2`, `1x3`, and `1x4` digits. Automatically handles integer/float numbers and decimal points (e.g., `12.34`, `0.5`).

#### Panel Text Labels (`ItemPanelText`)
- **Sizes:** `1x1`, `2x1`, `4x1`, `8x1` (full panel width), `2x2`, `4x2`.
- **Pure Text Display:** No bulky backing frame; renders crisp, high-visibility 3D text directly on the panel.
- **Customizable:** Text string, font scale, bold styling, and 24-bit RGB hex colors.

### Screwdriver & Configuration GUI (`ItemScrewdriver`)
- **Left-Click (LMB):** Safely dismantles the targeted component under the crosshair without breaking the panel block, popping the item into the player's inventory with sound feedback.
- **Right-Click (RMB):** Opens the modern Dark-Industrial Configuration GUI (350×240) with real-time live component preview, 12-color preset palette, hex color inputs, bold toggles, and size selectors.
- **HUD Debug Overlay:** Pointing a Screwdriver at any control panel displays a real-time HUD with component Slot ID, coordinates `(X, Y)`, type, dimensions, and active status.

### OpenComputers `control_panel` API Reference

| Method | Signature | Description |
| :--- | :--- | :--- |
| `clear` | `():boolean` | Removes all components from the panel. |
| `getElements` | `():table` | Returns array of all installed components with their slots, types, states, colors, labels, and coordinates. |
| `pressButton` | `(x:number, y:number):boolean` | Software-triggers a button press at grid `(x, y)`. |
| `getSwitch` | `(slot:number):boolean` | Returns the state (`true`/`false`) of a toggle switch. |
| `setSwitch` | `(slot:number, state:boolean):boolean` | Programmatically flips a toggle switch. |
| `setLamp` | `(x:number, y:number, state:boolean):boolean` | Sets lamp illumination on or off at `(x, y)`. |
| `setColor` | `(slot:number, color:number):boolean` | Sets base RGB color of any element. |
| `setSegment` | `(slot:number, value:any):boolean` | Updates the numeric value or string on a 7-segment display. |
| `getSegment` | `(slot:number):string` | Reads the current string on a 7-segment display. |
| `setText` | `(slot:number, text:string):boolean` | Updates the text label on a Button, Lamp, or Text element. |
| `getText` | `(slot:number):string` | Gets the text label of an element. |
| `setTextColor` | `(slot:number, color:number):boolean` | Sets text label RGB color. |
| `setTextScale` | `(slot:number, scale:number):boolean` | Sets font scale factor (0.2 to 4.0). |

---

## 3. Tape Drive & Digital Audio Streaming

### Overview
- **Block:** `computronics:tape_drive`
- Streams real uncompressed 8-bit digital audio directly from cassette items into the game world using OpenAL / Minecraft sound streams.
- Smooth mechanical insertion animation: inserting a cassette physically slides it deep into the drive chassis.

### Cassette Tapes

| Item | Capacity (Bytes) | Audio Duration | Crafting Material |
| :--- | :--- | :--- | :--- |
| **Iron Cassette** | ~1.92 MB | 4 minutes | Iron Ingot |
| **Gold Cassette** | ~3.84 MB | 8 minutes | Gold Ingot |
| **Diamond Cassette** | ~15.36 MB | 32 minutes | Diamond |
| **Nether Star Cassette** | ~61.44 MB | 128 minutes | Nether Star |

### OpenComputers `tape_drive` API Reference

| Method | Signature | Description |
| :--- | :--- | :--- |
| `isReady` | `():boolean` | Returns `true` if a cassette is inserted. |
| `isPlaying` | `():boolean` | Returns `true` if tape is currently playing. |
| `play` | `():boolean` | Starts playback at the current head position. |
| `stop` | `():boolean` | Stops playback. |
| `pause` | `():boolean` | Pauses playback. |
| `rewind` | `():boolean` | Rewinds the tape head back to position `0`. |
| `seek` | `(offset:number):number` | Seeks forward or backward by `offset` bytes. |
| `getPosition` | `():number` | Returns head position in bytes. |
| `getSize` | `():number` | Returns total cassette capacity in bytes. |
| `getLabel` | `():string` | Reads user-assigned cassette label. |
| `setLabel` | `(title:string):boolean` | Writes cassette label. |
| `setVolume` | `(vol:number):boolean` | Sets playback volume (0.0 to 1.0). |
| `setSpeed` | `(speed:number):boolean` | Sets playback speed / pitch multiplier (0.25 to 2.0). |
| `read` | `([count:number]):string` | Reads raw bytes from tape head. |
| `write` | `(data:string):number` | Writes binary audio data to cassette. |

---

## 4. Handheld Terminals & Pagers

### Pocket Terminal (`ItemPocketTerminal`)
- **Hold Style:** Held naturally in the player's right hand like a navigation map in a boat, tilted for clear visibility during movement.
- **No Animation Jitter:** Equipped matrix stays rock-solid while walking, running, or driving vehicles.
- **Display Matrix:** 32-column × 12-row dual-layer phosphor CRT terminal.
- **Wireless VRAM:** Embedded wireless receiver listens on modem ports (e.g. port 404).

### Inductive Charging
- Place an **Inductive Charger** (`computronics:inductive_charger`) under floors.
- Wirelessly recharges Pocket Terminals, robots, and wireless devices in a 3-block radius.

---

## 5. Server Rack Infrastructure

Computronics expansion boards install directly into OpenComputers Server Racks:

1. **Server Rack Light Board (`rack_board_light`):** Customizable RGB LEDs on the server rack chassis facing.
2. **Server Rack Switch Board (`rack_board_switch`):** Front-facing physical toggle switches to route server rack bus packets.
3. **Server Rack Capacitor (`rack_board_capacitor`):** High-density energy storage for heavy compute loads.
4. **Rack Cryptographic Co-Processor (`rack_board_co_processor`):** Hardware-accelerated MD5, SHA-256, CRC32, and AES-128 encryption.
5. **Rack KVM Switch Module (`rack_board_kvm`):** Interactive 4-channel KVM switch to toggle peripherals and keyboard/video streams between mounted servers.

---

## 6. Speech Box & Audio Upgrades

- **Speech Box (`computronics:speech_box`):** Real text-to-speech voice synthesizer. Speaks clear English/Russian text to all players within earshot.
- **Speech Upgrade (`computronics:speech_upgrade`):** Robot/Drone upgrade allowing autonomous units to speak out loud.
- **Clean Voice:** Annoying legacy bit-beeps have been removed; only clean synthesized speech audio plays.

---

## 7. OpenOS Software Suite: `computronics.lua`

The mod includes an extensive, turnkey Lua utility suite located at `/computronics.lua` on OpenOS disks:
- `computronics panel` — Interactive dashboard and live control panel debugger.
- `computronics tape <file.dfn>` — High-speed streaming cassette player and recorder.
- `computronics speech <text>` — Text-to-speech speaker utility.
- `computronics pager <message>` — Broadcast telemetry to handheld Pocket Terminals.
