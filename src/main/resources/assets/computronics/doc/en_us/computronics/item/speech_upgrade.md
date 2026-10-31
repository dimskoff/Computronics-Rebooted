# Speech Upgrade

Upgrade for robots and drones equipping them with robotic speech synthesis.

## Component Name
`speech`

## Methods
* `say(text: string, [pitch: number]): boolean` - Speaks text aloud.
* `getPitch(): number` - Gets current pitch.
* `setPitch(pitch: number): number` - Sets base pitch.
* `isSpeaking(): boolean` - Returns true if currently speaking.
* `stop(): boolean` - Stops speech.
