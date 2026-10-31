# Modular Control Panel

Interactive 8x8 control panel block holding up to 64 miniature LED lamps and tactile push buttons.

## Component Name
`control_panel`

## Interactions
* Right-click with `Miniature Indicator Lamp` or `Tactile Push Button` on face mounts element at raycasted slot.
* Sneak + Right-click dismantles element and drops it.
* Right-click on button presses it and fires network signal.

## Methods
* `setLampColor(slot: number, color: number): boolean` - Sets 24-bit RGB color.
* `getLampColor(slot: number): number` - Gets RGB color.
* `getElementType(slot: number): number` - Gets element type: 0 (empty), 1 (lamp), 2 (button).
* `getElements(): table` - Returns table of all 64 slots.
* `clear(): boolean` - Resets all lamp colors.
