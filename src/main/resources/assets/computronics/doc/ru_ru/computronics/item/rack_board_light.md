# Светодиодная плата для серверной стойки

Плата расширения для монтажа в **Серверную стойку** (Server Rack) с 4 программируемыми RGB-индикаторами.

Устанавливается в любой из 4 слотов стойки.

## Имя компонента
`light_board`

## Методы
* `setLightColor(index: number, color: number): boolean` — задаёт 24-битный цвет RGB индикатора (индексы от 0 до 3).
* `getLightColor(index: number): number` — возвращает цвет RGB индикатора.
* `setColors(colors: table): boolean` — устанавливает цвета всех индикаторов из таблицы (индексация с 1).
* `getLightCount(): number` — возвращает общее количество индикаторов (4).
* `setMode(mode: number): number` — задаёт режим работы/анимации (1–5).
* `getMode(): number` — возвращает текущий режим.
* `clear(): boolean` — выключает все индикаторы.

## Пример
```lua
local component = require("component")
local lights = component.light_board

-- Установить 1-й светодиод в зелёный, 2-й в красный
lights.setLightColor(0, 0x00FF00)
lights.setLightColor(1, 0xFF0000)
```
