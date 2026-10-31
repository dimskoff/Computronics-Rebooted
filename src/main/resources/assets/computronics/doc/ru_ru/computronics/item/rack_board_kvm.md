# Модуль KVM-переключателя для серверной стойки

Интеллектуальный 4-портовый модуль KVM (клавиатура, видео, мышь) для **Серверной стойки** OpenComputers.

Позволяет централизованно управлять до 4 серверными лезвиями в одной стойке. Игрок может переключать фокус управления между серверами кликом мыши прямо по модулю на лицевой панели стойки, либо скриптами из OpenOS.

## Имя компонента
`rack_kvm`

## Методы
* `getSelectedServer(): number` — возвращает индекс текущего активного сервера (0–3).
* `setSelectedServer(slot: number): boolean` — переключает фокус на указанный сервер стойки (0–3).
* `getChannelCount(): number` — возвращает количество каналов (4).

## Сигналы (события)
При смене активного сервера (вручную кликом по стойке или через `setSelectedServer`):
* `rack_kvm_switch(slot: number)`

## Пример
```lua
local component = require("component")
local event = require("event")
local kvm = component.rack_kvm

-- Переключить KVM на сервер №2
kvm.setSelectedServer(2)
print("Активный сервер KVM: " .. kvm.getSelectedServer())
```
