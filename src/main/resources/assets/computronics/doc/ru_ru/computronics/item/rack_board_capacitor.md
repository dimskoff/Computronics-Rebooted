# Конденсаторная плата для серверной стойки

Высокоёмкостный энергомодуль для монтажа в **Серверную стойку** (Server Rack).

Устанавливается в любой из 4 слотов стойки. Служит встроенным источником бесперебойного питания (ИБП / UPS) ёмкостью 50 000 EU, защищая серверные лезвия и сетевые платы от перебоев в электросети.

## Имя компонента
`capacitor_board`

## Методы
* `getEnergy(): number` — возвращает текущий запас накопленной энергии в буфере (в EU).
* `getMaxEnergy(): number` — возвращает максимальную ёмкость конденсатора (50 000 EU).

## Пример
```lua
local component = require("component")
local cap = component.capacitor_board

local energy = cap.getEnergy()
local maxEnergy = cap.getMaxEnergy()
print(string.format("Резерв энергии стойки: %.0f / %.0f EU (%.1f%%)", energy, maxEnergy, (energy / maxEnergy) * 100))
```
