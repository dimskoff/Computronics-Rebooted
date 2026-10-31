# Железный нотный блок (Iron Note Block)

Улучшенный нотный блок, способный программно проигрывать звуки всех 16 инструментов Minecraft на любых нотах без необходимости менять под ним блоки.

## Имя компонента
`iron_noteblock`

## Методы

* `playNote(instrument: number|string, note: number, [volume: number]): boolean`
  Проигрывает ноту на указанном инструменте (инструмент 0-15 или по имени: `harp`, `bass`, `snare`, `bell`, `chime`, `flute`, `guitar`, `xylophone`, `iron_xylophone`, `cow_bell`, `didgeridoo`, `bit`, `banjo`, `pling`). Нота от 0 до 24 полутонов.
* `getInstruments(): table`
  Возвращает список всех доступных инструментов.
