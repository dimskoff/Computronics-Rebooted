# Акустический датчик (Acoustic Sensor)

**Акустический датчик** — сенсор вибраций и звуков, использующий современную систему GameEvent вибраций Minecraft 1.21.1 (аналогично скалк-сенсорам). Улавливает шаги, звуки открытия дверей, взрывы, падения снарядов и разрушение блоков.

Имя компонента: `acoustic_sensor`

## Особенности обнаружения
- **Скрытность (Shift) и шерсть**: Игроки, крадущиеся на шифте, а также движение по шерсти или коврам не генерируют звуковых волн и не вызывают срабатывания датчика.

## Методы компонента

- `getSensitivity(): number`  
  Возвращает текущую чувствительность/радиус сенсора (по умолчанию: 16 блоков, макс: 32).
- `setSensitivity(range: number): number`  
  Устанавливает радиус обнаружения (от `1` до `32` блоков).
- `getLastVibration(): table`  
  Возвращает таблицу с детальной информацией о последней зафиксированной вибрации (`event`, `distance`, `frequency`, координаты `x`, `y`, `z`, имя `entity`).

## Сигналы (Events)

- `vibration(event: string, distance: number, frequency: number, x: number, y: number, z: number)`  
  Сигнал, отправляемый компьютеру при фиксации акустической волны.

## Пример на Lua

```lua
local event = require("event")
local component = require("component")
local sensor = component.acoustic_sensor

sensor.setSensitivity(20)
print("Акустическая сигнализация включена. Ожидание звуков...")

while true do
  local _, ev, dist, freq, x, y, z = event.pull("vibration")
  print(string.format("Зафиксирован звук: %s (Частота: %d) на дистанции %.1fм!", ev, freq, dist))
end
```
