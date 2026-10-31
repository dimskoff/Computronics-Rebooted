# Плата переключателей для серверной стойки

Плата расширения для монтажа в **Серверную стойку** (Server Rack) с 4 физическими тумблерами-переключателями.

Устанавливается в любой из 4 слотов стойки. Игроки могут переключать тумблеры напрямую правым кликом мыши по лицевой панели стойки, либо компьютеры могут управлять ими программно.

## Имя компонента
`switch_board`

## Методы
* `getSwitch(index: number): boolean` — возвращает состояние переключателя (индексы от 0 до 3).
* `setSwitch(index: number, state: boolean): boolean` — программно переключает тумблер и генерирует событие `rack_switch`.
* `getSwitchCount(): number` — возвращает общее количество тумблеров (4).

## Сигналы (события)
При физическом переключении игроком на стойке или через `setSwitch`:
* `rack_switch(index: number, state: boolean)`

```lua
local event = require("event")
while true do
  local _, idx, state = event.pull("rack_switch")
  print(string.format("Тумблер %d переключен в %s", idx, tostring(state)))
end
```
