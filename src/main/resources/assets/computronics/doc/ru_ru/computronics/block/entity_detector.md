# Детектор сущностей (Entity Detector)

Детектор сущностей сканирует пространство перед собой в заданном конусе и возвращает подробные данные обо всех найденных сущностях и игроках.

## Имя компонента
`entity_detector`

## Методы

* `getFacing(): string`
  Возвращает направление взгляда детектора (`north`, `south`, `west`, `east`).
* `getRange(): number`
  Возвращает текущую дальность сканирования.
* `setRange(range: number): number`
  Устанавливает дальность сканирования (от 1 до 32 блоков).
* `count([range: number]): number`
  Возвращает количество любых сущностей в конусе детектора.
* `getPlayerCount([range: number]): number`
  Возвращает количество игроков в конусе детектора.
* `isPlayerInRange([range: number]): boolean`
  Быстрая проверка: находится ли хотя бы один игрок в зоне видимости.
* `scan([range: number]): table`
  Выполняет полное сканирование и возвращает массив таблиц:
  - `name`: имя сущности
  - `uuid`: уникальный UUID
  - `type`: реестровый тип (`minecraft:player`, `minecraft:zombie` и т.д.)
  - `distance`: точное евклидово расстояние
  - `x`, `y`, `z`: относительные смещения от детектора
  - `isPlayer`: флаг игрока
  - `health`, `maxHealth`: здоровье
  - `armor`: показатель брони
  - `mainHand`: предмет в главной руке
* `setSignalEnabled(enabled: boolean): boolean`
  Включает автоматическую генерацию сигналов `entity_detected` при появлении новых сущностей в зоне.
* `isSignalEnabled(): boolean`
  Проверяет, включена ли отправка сигналов.

## Сигналы
При включенном `setSignalEnabled(true)` детектор генерирует сигнал в компьютерную сеть:
`entity_detected(name: string, uuid: string, distance: number, isPlayer: boolean)`
