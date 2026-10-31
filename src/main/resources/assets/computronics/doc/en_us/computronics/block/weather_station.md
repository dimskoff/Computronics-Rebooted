# Weather Station

The **Weather Station** monitors local atmospheric conditions, ambient light, precipitation strength, temperature, and planetary celestial cycles. It can also calculate short-term weather forecasts.

Component name: `weather_station`

## Methods

- `isRaining(): boolean`  
  Checks if it is currently raining or snowing.
- `isThundering(): boolean`  
  Checks if a thunderstorm is active.
- `getRainStrength(): number`  
  Gets current precipitation intensity (`0.0` to `1.0`).
- `getThunderStrength(): number`  
  Gets storm intensity (`0.0` to `1.0`).
- `getTemperature(): number`  
  Gets ambient biome base temperature at the station's location.
- `hasPrecipitation(): boolean`  
  Checks if current biome supports rainfall or snowfall.
- `getLightLevel(): number`  
  Gets total raw brightness above the station (`0` to `15`).
- `getMoonPhase(): number`  
  Gets current lunar phase (`0` Full Moon, `4` New Moon).
- `predictStorm(): table`  
  Queries atmospheric server data for storm prediction:
  ```lua
  {
    isRaining = false,
    isThundering = false,
    clearWeatherTicks = 12000,
    rainTicks = 0,
    thunderTicks = 0
  }
  ```

## Lua Example

```lua
local component = require("component")
local ws = component.weather_station

local forecast = ws.predictStorm()
print(string.format("Weather: Rain=%s, Thunder=%s, Temp=%.2f, MoonPhase=%d",
  tostring(ws.isRaining()), tostring(ws.isThundering()), ws.getTemperature(), ws.getMoonPhase()))

if forecast and forecast.clearWeatherTicks then
  print(string.format("Clear skies expected for next %.1f minutes.", forecast.clearWeatherTicks / 1200))
end
```
