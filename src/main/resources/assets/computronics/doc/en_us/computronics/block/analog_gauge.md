# Analog Gauge

The **Analog Gauge** is a physical dial meter for industrial control rooms and vehicle dashboards. The needle indicator can be positioned anywhere from `0.0` (empty/zero) to `1.0` (full/max).

Component name: `analog_gauge`

## Methods

- `getValue(): number`  
  Returns the current needle position (`0.0` to `1.0`).
- `setValue(val: number): number`  
  Sets the gauge position between `0.0` and `1.0`. Updates visual dial.
- `getLabel(): string` / `setLabel(text: string): string`  
  Gets or sets the custom text label displayed on the meter.
- `getColor(): number` / `setColor(rgb: number): number`  
  Gets or sets backlight RGB color integer (e.g. `0xFF0000` for red warning).

## Lua Example

```lua
local component = require("component")
local gauge = component.analog_gauge

gauge.setLabel("Reactor Core Temp")
gauge.setColor(0xFF8800) -- Orange backlight

-- Set gauge to 75%
gauge.setValue(0.75)
print("Gauge updated to 75%.")
```
