# Сниффер пакетов (Packet Sniffer)

**Плата анализа трафика (Packet Sniffer)** — сетевая карта аппаратного перехвата данных (аппаратный TAP). Позволяет в неразборчивом режиме (Promiscuous mode) перехватывать и анализировать весь сетевой трафик, проходящий по кабелям, даже если порты на принимающем компьютере не открыты.

Имя компонента: `packet_sniffer`

## Возможности
- **Зеркалирование (Promiscuous)**: Включает перехват как широковещательных, так и прямых адресных пакетов между чужими компьютерами.
- **Телеметрия сети**: В реальном времени замеряет скорость трафика (Байт/с — BPS) и частоту пакетов (Пакетов/с — PPS).
- **Фильтрация**: Позволяет фильтровать пакеты по номеру порта или по префиксу адреса отправителя.

## Методы компонента

- `getBandwidth(): number`  
  Возвращает текущую сетевую нагрузку в Байтах в секунду (BPS).
- `getPacketsPerSecond(): number`  
  Возвращает частоту пакетов в секунду (PPS).
- `getTotalBytes(): number`  
  Возвращает суммарный объём перехваченных байт.
- `getTotalPackets(): number`  
  Возвращает суммарное количество перехваченных пакетов.
- `isPromiscuous(): boolean` / `setPromiscuous(state: boolean): boolean`  
  Включает/выключает режим полного зеркалирования всех проходящих пакетов.
- `setFilterPort(port: number): number`  
  Устанавливает фильтр по порту (значение `0` слушает все порты).
- `setFilterSender(sender: string): string`  
  Фильтрует пакеты только от указанного адреса/префикса.
- `resetStats(): boolean`  
  Сбрасывает счётчики статистики.

## Сигналы (Events)

- `packet_sniffed(source: string, destination: string, port: number, size: number, summary: string)`  
  Генерируется компьютером при перехвате каждого пакета в сети. Содержит адрес отправителя, получателя, порт, размер и превью содержимого.

## Пример на Lua

```lua
local event = require("event")
local component = require("component")
local sniffer = component.packet_sniffer

sniffer.setPromiscuous(true)
print("Сниффер запущен. Прослушивание кабеля...")

while true do
  local _, src, dst, port, size, content = event.pull("packet_sniffed")
  print(string.format("[%s -> %s] Порт %d (%d байт): %s", src:sub(1,6), dst:sub(1,6), port, size, content))
  print(string.format("  Трафик: %d Б/с | Частота: %d пак/с", sniffer.getBandwidth(), sniffer.getPacketsPerSecond()))
end
```
