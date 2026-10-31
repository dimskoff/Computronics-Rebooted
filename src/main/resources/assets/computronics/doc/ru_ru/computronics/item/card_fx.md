# Карта эффектов и частиц (Particle FX Card)

Карта расширения, позволяющая компьютеру испускать разнообразные визуальные эффекты и современные частицы.

## Имя компонента
`particle_fx`

## Методы

* `spawn(type: string, [count: number], [offsetX: number, offsetY: number, offsetZ: number], [speed: number]): boolean`
  Испускает частицы указанного типа вокруг хоста.
* `getTypes(): table`
  Возвращает список поддерживаемых типов: `flame`, `smoke`, `portal`, `enchant`, `heart`, `crit`, `electric_spark`, `sculk_charge`, `sonic_boom`, `cherry_leaves`, `glow`, `soul_fire_flame`, `witch`, `note`.
* `getParticleLimit(): number`
  Возвращает максимум частиц за один вызов (100).
