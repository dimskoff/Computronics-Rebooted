# Iron Note Block

Programmable note block capable of playing all 16 Minecraft instruments across 2 octaves without requiring specific base blocks.

## Component Name
`iron_noteblock`

## Methods
* `playNote(instrument: number|string, note: number, [volume: number]): boolean` - Plays note (0-24 semitones) on instrument (0-15 or instrument name).
* `getInstruments(): table` - Returns list of all instruments.
