# Метеостанция (Weather Station)

**Метеостанция** — блок метеорологического мониторинга окружающей среды. Отслеживает силу дождя, грозы, температуру биома, фазы луны, уровень освещения и рассчитывает прогноз погоды.

Имя компонента: `weather_station`

## Методы компонента

- `isRaining(): boolean`  
  Проверяет, идёт ли дождь или снегопад.
- `isThundering(): boolean`  
  Проверяет, идёт ли гроза.
- `getRainStrength(): number`  
  Возвращает силу дождя от `0.0` до `1.0`.
- `getThunderStrength(): number`  
  Возвращает интенсивность грозы от `0.0` до `1.0`.
- `getTemperature(): number`  
  Возвращает базовую температуру биома в точке установки.
- `hasPrecipitation(): boolean`  
  Проверяет, возможны ли осадки в данном биоме.
- `getLightLevel(): number`  
  Возвращает уровень освещённости над станцией (`0`–`15`).
- `getMoonPhase(): number`  
  Возвращает текущую фазу луны (`0` полнолуние, `4` новолуние).
- `predictStorm(): table`  
  Возвращает прогноз погоды: сколько тиков осталось до дождя (`clearWeatherTicks`), длительность дождя (`rainTicks`) и грозы (`thunderTicks`).

## Пример на Lua

```lua
local component = require("component")
local ws = component.weather_station

local forecast = ws.predictStorm()
print("Температура воздуха: " .. ws.getTemperature())
print("Фаза луны: " .. ws.getMoonPhase())

if forecast and forecast.clearWeatherTicks > 0 then
  print(string.format("Ясная погода продлится ещё %.1f мин.", forecast.clearWeatherTicks / 1200))
end
```
