# Чат-бокс (Chat Box)

**Чат-бокс** позволяет компьютеру взаимодействовать с игровым чатом Minecraft: отправлять публичные объявления, писать личные сообщения игрокам и отслеживать реплики пользователей.

Имя компонента: `chat_box`

## Методы компонента

- `say(message: string[, distance: number]): boolean`  
  Отправляет сообщение в общий чат (или ограниченно в радиусе `distance` блоков).
- `tell(player: string, message: string): boolean`  
  Отправляет приватное сообщение указанному игроку. Возвращает `true`, если игрок найден на сервере.
- `getDistance(): number`  
  Возвращает текущую дальность вещания и прослушивания чата (по умолчанию: 64 блока).
- `setDistance(distance: number): number`  
  Устанавливает максимальную дальность в блоках. Значение `0` снимает ограничение на весь мир.
- `getName(): string`  
  Возвращает текущий префикс чат-бокса.
- `setName(name: string): string`  
  Устанавливает префикс отправителя (например, `"[База Омега]"`).

## Сигналы (Events)

- `chat_message(player: string, message: string, distance: number)`  
  Генерируется компьютером всякий раз, когда игрок пишет сообщение в радиусе действия чат-бокса.

## Пример чат-бота на Lua

```lua
local event = require("event")
local component = require("component")
local chat = component.chat_box

chat.setName("[ИИ Базы]")
chat.say("Система активирована. Напишите '!статус' в чат.")

while true do
  local _, player, text, dist = event.pull("chat_message")
  print(string.format("[%s]: %s (дистанция: %.1f)", player, text, dist))

  if text == "!статус" then
    chat.tell(player, "Все системы базы функционируют штатно.")
  end
end
```
