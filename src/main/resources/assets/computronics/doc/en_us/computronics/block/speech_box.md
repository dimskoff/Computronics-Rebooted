# Speech Box

Phonetic voice synthesizer capable of speaking arbitrary text and broadcasting subtitles within 32 meters.

## Component Name
`speech_box`

## Methods
* `say(text: string, [pitch: number]): boolean` - Speaks text with optional pitch (0.5 - 2.0).
* `getPitch(): number` - Gets current pitch.
* `setPitch(pitch: number): number` - Sets base pitch.
* `isSpeaking(): boolean` - Returns true if currently speaking.
* `stop(): boolean` - Stops speech.
