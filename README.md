# Computronics: Rebooted

An [OpenComputers: Rebooted](https://github.com/PC-Logix/OpenComputers) addon for **Minecraft 1.21.1 / NeoForge**,
continuing the original Computronics by asie and Vexatos.

## Requirements

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x |
| OpenComputers: Rebooted | 1.9.4 or newer |

## Features

**Blocks (OC components):** Tape Drive, Chat Box, Speech Box, Iron Note Block, Camera, Radar, Entity Detector,
Acoustic Sensor, Weather Station, Laser Transceiver, RF Jammer, Inductive Charger, Analog Gauge, Control Panel.

**Cards:** Beep Card, Particle (FX) Card, Self-Destructing Card, RF Jammer Card, Tier 3 Network Card, Packet Sniffer.

**Robot / drone upgrades:** Camera, Chat, Radar, Speech and Winch upgrades.

**Server rack mountables:** Light Board, Switch Board, Capacitor Board, Boom Board, Crypto Co-Processor.

**Other:** Pocket Terminal, Control Panel elements (buttons, switches, lamps, 7-segment displays, text) with a Screwdriver,
cassette tapes in four tiers. Recipes use OpenComputers parts (microchips, printed circuit boards, card bases).
Every block and item has a page in the in-game OpenComputers manual (English and Russian).

## Tapes

| Tape | Size | Length (DFPWM, 32 kHz) |
|---|---|---|
| Iron | 1 MB | 4:16 |
| Gold | 2 MB | 8:32 |
| Diamond | 4 MB | 17:04 |
| Nether Star | 16 MB | 68:16 |

Audio is stored as DFPWM at 32768 Hz (4096 bytes per second). Convert any audio file with ffmpeg:

```
ffmpeg -i in.mp3 -ac 1 -ar 32768 -f dfpwm out.dfpwm
```

Copy the file to the computer (e.g. via `wget` with an Internet Card or by placing it in the save's OC disk folder),
then use the `tape` program from the Computronics loot disk:

```
tape write out.dfpwm      -- write file to the inserted tape
tape label "My Song"      -- name the tape
tape play / tape stop / tape rewind
```

## Building

```
./gradlew build
```

`libs/opencomputers-1.9.4-5.jar` is required to compile and is loaded automatically by `./gradlew runClient`.

## License

MIT, see [LICENSE](LICENSE).
